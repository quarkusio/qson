package io.quarkus.qson.resteasy.deployment;

import org.jboss.jandex.DotName;

public final class DotNames {

    private DotNames() {
    }

    static final DotName CONSUMES = DotName.createSimple("javax.ws.rs.Consumes");
    static final DotName PRODUCES = DotName.createSimple("javax.ws.rs.Produces");
    static final DotName GET = DotName.createSimple("javax.ws.rs.GET");
    static final DotName DELETE = DotName.createSimple("javax.ws.rs.DELETE");
    static final DotName PATCH = DotName.createSimple("javax.ws.rs.PATCH");
    static final DotName POST = DotName.createSimple("javax.ws.rs.POST");
    static final DotName PUT = DotName.createSimple("javax.ws.rs.PUT");
}
