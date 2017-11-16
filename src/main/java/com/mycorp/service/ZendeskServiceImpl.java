package com.mycorp.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.Zendesk;
import com.mycorp.dto.DatosClienteDTO;
import com.mycorp.dto.PolizaDTO;
import com.mycorp.dto.TicketDTO;
import com.mycorp.dto.ValueCodeDTO;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;
import com.mycorp.constants.MyCorrpConstants;

import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

@Service
public class ZendeskServiceImpl {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger( ZendeskService.class );


    @Value("#{envPC['zendesk.ticket']}")
    public String PETICION_ZENDESK= "";

    @Value("#{envPC['zendesk.token']}")
    public String TOKEN_ZENDESK= "";

    @Value("#{envPC['zendesk.url']}")
    public String URL_ZENDESK= "";

    @Value("#{envPC['zendesk.user']}")
    public String ZENDESK_USER= "";

    @Value("#{envPC['tarjetas.getDatos']}")
    public String TARJETAS_GETDATOS = "";

    @Value("#{envPC['cliente.getDatos']}")
    public String CLIENTE_GETDATOS = "";

    @Value("#{envPC['zendesk.error.mail.funcionalidad']}")
    public String ZENDESK_ERROR_MAIL_FUNCIONALIDAD = "";

    @Value("#{envPC['zendesk.error.destinatario']}")
    public String ZENDESK_ERROR_DESTINATARIO = "";

    private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");


    /** The portalclientes web ejb remote. */
    @Autowired
    // @Qualifier("portalclientesWebEJB")
    private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier( "emailService" )
    MensajeriaService emailService;

    /**
     * Crea un ticket en Zendesk. Si se ha informado el nÂº de tarjeta, obtiene los datos asociados a dicha tarjeta de un servicio externo.
     * @param usuarioAlta
     * @param userAgent
     */
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent){

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        StringBuilder datosUsuario = new StringBuilder();
        StringBuilder datosBravo = new StringBuilder();

        String idCliente = null;

        StringBuilder clientName = new StringBuilder();


        // AÃ±ade los datos del formulario
        añadirDatosFormulario(usuarioAlta, userAgent, datosUsuario);

        
        StringBuilder datosServicio = new StringBuilder();
        // Obtiene el idCliente de la tarjeta
        if(StringUtils.isNotBlank(usuarioAlta.getNumTarjeta())){
            try{
                String urlToRead = TARJETAS_GETDATOS + usuarioAlta.getNumTarjeta();
                ResponseEntity<String> res = restTemplate.getForEntity( urlToRead, String.class);
                if(res.getStatusCode() == HttpStatus.OK){
                    String dusuario = res.getBody();
                    clientName.append(dusuario);
                    idCliente = dusuario;
                    datosServicio.append("Datos recuperados del servicio de tarjeta:").append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(dusuario));
                }
            }catch(Exception e)
            {
                LOG.error("Error al obtener los datos de la tarjeta", e);
            }
        }
        else if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
            try
            {
                PolizaDTO poliza = new PolizaDTO();
                poliza.setNumPoliza(Integer.valueOf(usuarioAlta.getNumPoliza()));
                poliza.setNumColectivo(Integer.valueOf(usuarioAlta.getNumDocAcreditativo()));
                poliza.setCompania(1);

                PolizaBasico polizaBasicoConsulta = new PolizaBasicoFromPolizaBuilder().withPoliza( poliza ).build();

                final util.datos.DetallePoliza detallePolizaResponse = portalclientesWebEJBRemote.recuperarDatosPoliza(polizaBasicoConsulta);

                clientName.append(detallePolizaResponse.getTomador().getNombre()).
                            append(" ").
                            append(detallePolizaResponse.getTomador().getApellido1()).
                            append(" ").
                            append(detallePolizaResponse.getTomador().getApellido2());

                idCliente = detallePolizaResponse.getTomador().getIdentificador();
                datosServicio.append("Datos recuperados del servicio de tarjeta:").append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(detallePolizaResponse));
            }catch(Exception e)
            {
                LOG.error("Error al obtener los datos de la poliza", e);
            }
        }

        try
        {
            // Obtenemos los datos del cliente
            obtenerDatosCliente(datosBravo, idCliente);


        }catch(Exception e)
        {
            LOG.error("Error al obtener los datos en BRAVO del cliente", e);
        }

        String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(), datosUsuario.toString()+datosBravo.toString()+
                parseJsonBravo(datosServicio));
        ticket = ticket.replaceAll("["+MyCorrpConstants.ESCAPED_LINE_SEPARATOR+"]", " ");

        try(Zendesk zendesk = new Zendesk.Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK).build()){
            //TicketDTO
            TicketDTO petiZendesk = mapper.readValue(ticket, TicketDTO.class);
            zendesk.createTicket(petiZendesk);

        }catch(Exception e){
            LOG.error("Error al crear ticket ZENDESK", e);
            // Send email

            CorreoElectronico correo = new CorreoElectronico( Long.parseLong(ZENDESK_ERROR_MAIL_FUNCIONALIDAD), "es" )
                    .addParam(datosUsuario.toString().replaceAll(MyCorrpConstants.ESCAPE_ER+MyCorrpConstants.ESCAPED_LINE_SEPARATOR, MyCorrpConstants.HTML_BR))
                    .addParam(datosBravo.toString().replaceAll(MyCorrpConstants.ESCAPE_ER+MyCorrpConstants.ESCAPED_LINE_SEPARATOR, MyCorrpConstants.HTML_BR));
            correo.setEmailA( ZENDESK_ERROR_DESTINATARIO );
            try
            {
                emailService.enviar( correo );
            }catch(Exception ex){
                LOG.error("Error al enviar mail", ex);
            }

        }

        datosUsuario.append(datosBravo);

        return datosUsuario.toString();
    }

	private void añadirDatosFormulario(UsuarioAlta usuarioAlta, String userAgent, StringBuilder datosUsuario) {
		if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
            datosUsuario.append("NÂº de poliza/colectivo: ").append(usuarioAlta.getNumPoliza()).append("/").append(usuarioAlta.getNumDocAcreditativo()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        }else{
            datosUsuario.append("NÂº tarjeta Sanitas o Identificador: ").append(usuarioAlta.getNumTarjeta()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        }
        datosUsuario.append("Tipo documento: ").append(usuarioAlta.getTipoDocAcreditativo()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("NÂº documento: ").append(usuarioAlta.getNumDocAcreditativo()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("Email personal: ").append(usuarioAlta.getEmail()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("NÂº mÃ³vil: ").append(usuarioAlta.getNumeroTelefono()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("User Agent: ").append(userAgent).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
	}

	private void obtenerDatosCliente(StringBuilder datosBravo, String idCliente) throws ParseException {
		DatosClienteDTO cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosClienteDTO.class, idCliente);
		datosBravo.append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:" + MyCorrpConstants.ESCAPED_LINE_SEPARATOR + MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
		datosBravo.append("TelÃ©fono: ").append(cliente.getGenTGrupoTmk()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);


		datosBravo.append("Feha de nacimiento: ").append(formatter.format(formatter.parse(cliente.getFechaNacimiento()))).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);

		List< ValueCodeDTO > tiposDocumentos = getTiposDocumentosRegistro();
		for(int i = 0; i < tiposDocumentos.size();i++)
		{
		    if(tiposDocumentos.get(i).getCode().equals(cliente.getGenCTipoDocumento().toString()))
		    {
		        datosBravo.append("Tipo de documento: ").append(tiposDocumentos.get(i).getValue()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
		    }
		}
		datosBravo.append("NÃºmero documento: ").append(cliente.getNumeroDocAcred()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);

		datosBravo.append("Tipo cliente: ");
		switch (cliente.getGenTTipoCliente()) {
		case 1:
		    datosBravo.append("POTENCIAL").append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
		    break;
		case 2:
		    datosBravo.append("REAL").append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
		    break;
		case 3:
		    datosBravo.append("PROSPECTO").append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
		    break;
		}

		datosBravo.append("ID estado del cliente: ").append(cliente.getGenTStatus()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);

		datosBravo.append("ID motivo de alta cliente: ").append(cliente.getIdMotivoAlta()).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR);

		datosBravo.append("Registrado: ").append((cliente.getfInactivoWeb() == null ? "SÃ­" : "No")).append(MyCorrpConstants.ESCAPED_LINE_SEPARATOR + MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
	}

    public List< ValueCodeDTO > getTiposDocumentosRegistro() {
        return Arrays.asList( new ValueCodeDTO(), new ValueCodeDTO() ); // simulacion servicio externo
    }

    /**
     * MÃ©todo para parsear el JSON de respuesta de los servicios de tarjeta/pÃ³liza
     *
     * @param resBravo
     * @return
     */
    private String parseJsonBravo(StringBuilder resBravo)
    {
        return resBravo.toString().replaceAll("[\\[\\]\\{\\}\\\"\\r]", "").replaceAll(MyCorrpConstants.ESCAPED_LINE_SEPARATOR, MyCorrpConstants.ESCAPE_ER + MyCorrpConstants.ESCAPED_LINE_SEPARATOR);
    }
}