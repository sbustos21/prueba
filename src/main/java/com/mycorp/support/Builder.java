package com.mycorp.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.Zendesk;
import com.mycorp.dto.TicketDTO;
import com.mycorp.util.ZendeskUtility;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.Response;;


    public class Builder {
        private AsyncHttpClient client = null;
        private final String url;
        private String username = null;
        private String password = null;
        private String token = null;
        private String oauthToken = null;

        public Builder(String url) {
            this.url = url;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            if (password != null) {
                this.token = null;
                this.oauthToken = null;
            }
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            if (token != null) {
                this.password = null;
                this.oauthToken = null;
            }
            return this;
        }

        public Zendesk build() {
            if (token != null) {
                return new Zendesk(client, url, username + "/token", token);
            }
            return new Zendesk(client, url, username, password);
        }
    }