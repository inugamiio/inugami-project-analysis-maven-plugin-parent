package io.inugami.maven.plugin.analysis.front.core.servlet;

import javax.servlet.http.HttpServletResponse;

public class ServletCommons {
    private static final String UTF_8 = "utf-8";

    public static void setUtf8(final HttpServletResponse response) {
        response.setCharacterEncoding(UTF_8);
        response.addHeader("Content-Encoding", UTF_8);
    }
}
