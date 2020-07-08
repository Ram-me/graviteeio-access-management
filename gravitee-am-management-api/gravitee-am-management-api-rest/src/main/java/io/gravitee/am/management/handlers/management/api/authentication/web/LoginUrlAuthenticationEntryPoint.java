/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.management.handlers.management.api.authentication.web;

import io.gravitee.am.management.handlers.management.api.authentication.provider.generator.RedirectCookieGenerator;
import io.gravitee.common.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoginUrlAuthenticationEntryPoint extends org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint {

    private static final String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";

    @Autowired
    private RedirectCookieGenerator redirectCookieGenerator;

    public LoginUrlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        String url = super.buildRedirectUrlToLoginPage(request, response, authException);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

        String scheme = request.getHeader(HttpHeaders.X_FORWARDED_PROTO);
        if (scheme != null && !scheme.isEmpty()) {
            builder.scheme(scheme);
        } else {
            scheme = request.getScheme();
        }

        String host = request.getHeader(HttpHeaders.X_FORWARDED_HOST);
        int port = "https".equalsIgnoreCase(scheme) ? 443 : 80;

        if (host == null || host.isEmpty()) {
            host = request.getHeader(HttpHeaders.HOST);
        }

        if (host.contains(":")) {
            // Forwarded host contains both host and port
            String[] parts = host.split(":");
            builder.host(parts[0]);
            builder.port(parts[1]);
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            builder.host(host);
        }

        // handle forwarded path
        String forwardedPath = request.getHeader(X_FORWARDED_PREFIX);
        if (forwardedPath != null && !forwardedPath.isEmpty()) {
            String path = builder.build().getPath();
            // remove trailing slash
            forwardedPath = forwardedPath.substring(0, forwardedPath.length() - (forwardedPath.endsWith("/") ? 1 : 0));
            builder.replacePath(forwardedPath + path);
        }

        // Use a cookie to save the original redirect uri.
        response.addCookie(redirectCookieGenerator.generateCookie(UrlUtils.buildFullRequestUrl(scheme, host, port, request.getRequestURI(), request.getQueryString())));

        return builder.toUriString();
    }
}
