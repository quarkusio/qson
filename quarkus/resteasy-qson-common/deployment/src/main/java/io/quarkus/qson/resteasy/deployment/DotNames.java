package io.quarkus.qson.resteasy.deployment;

import org.jboss.jandex.DotName;

public final class DotNames {

    private DotNames() {
    }

    static final DotName CONSUMES = DotName.createSimple("jakarta.ws.rs.Consumes");
    static final DotName PRODUCES = DotName.createSimple("jakarta.ws.rs.Produces");
    static final DotName GET = DotName.createSimple("jakarta.ws.rs.GET");
    static final DotName DELETE = DotName.createSimple("jakarta.ws.rs.DELETE");
    static final DotName PATCH = DotName.createSimple("jakarta.ws.rs.PATCH");
    static final DotName POST = DotName.createSimple("jakarta.ws.rs.POST");
    static final DotName PUT = DotName.createSimple("jakarta.ws.rs.PUT");
}
