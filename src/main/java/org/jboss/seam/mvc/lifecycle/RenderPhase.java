/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.mvc.lifecycle;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.sun.org.apache.xpath.internal.functions.FuncUnparsedEntityURI;
import org.jboss.seam.mvc.template.ELVariableResolverFactory;
import org.mvel2.ParserContext;
import org.mvel2.integration.impl.FunctionVariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolver;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateTools;
import org.mvel2.util.MethodStub;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class RenderPhase implements Phase {
    private final ELVariableResolverFactory factory;
    private TemplateRegistry registry;
    private MapVariableResolverFactory functions;


    @Inject
    public RenderPhase(final ELVariableResolverFactory factory) {
        this.factory = factory;

        // create a map resolve to hold the functions we want to inject, and chain
        // the ELVariableResolverFactory to this factory.
        functions = new MapVariableResolverFactory(new HashMap<String, Object>(), factory);
    }

    @PostConstruct
    public void init() {
        try {

            // inject the method by wrapping it in a MethodStub -- this is a marking wrapper that tells
            // mvel internally to treat this is a function pointer.
            functions.createVariable("time", new MethodStub(System.class.getMethod("bind", this.getClass())));
        } catch (NoSuchMethodException e) {
            // handle exception here.
        }

        registry = new SimpleTemplateRegistry();
        registry.addNamedTemplate("forms",
                TemplateCompiler.compileTemplate("@code{def bind(loc) {System.out.println(loc);}}"));
    }

    public String perform(final InputStream stream, final Map context) {
        String template = TemplateTools.readStream(stream);

        // evaulated the template with the functions resolver as the outer resolver
        // remember we chained 'functions' to 'factory'
        return (String) TemplateRuntime.eval(template, context, functions, registry);
    }
}
