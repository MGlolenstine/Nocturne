/*
 * Nocturne
 * Copyright (c) 2015-2016, Lapis <https://github.com/LapisBlue>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package blue.lapis.nocturne.mapping.io.writer;

import blue.lapis.nocturne.mapping.MappingContext;
import blue.lapis.nocturne.mapping.model.ClassMapping;
import blue.lapis.nocturne.mapping.model.FieldMapping;
import blue.lapis.nocturne.mapping.model.MethodMapping;
import blue.lapis.nocturne.mapping.model.MethodParameterMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The mappings writer, for the SRG format.
 */
public class JamWriter extends MappingsWriter {

    private final ByteArrayOutputStream clOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream fdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream mdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream mpOut = new ByteArrayOutputStream();

    private final PrintWriter clWriter = new PrintWriter(clOut);
    private final PrintWriter fdWriter = new PrintWriter(fdOut);
    private final PrintWriter mdWriter = new PrintWriter(mdOut);
    private final PrintWriter mpWriter = new PrintWriter(mpOut);

    /**
     * Constructs a new {@link JamWriter} which outputs to the given
     * {@link PrintWriter}.
     *
     * @param out The {@link PrintWriter} to output to
     */
    public JamWriter(PrintWriter out) {
        super(out);
    }

    @Override
    public void write(MappingContext mappingContext) {
        mappingContext.getMappings().values().forEach(this::writeClassMapping);
        clWriter.close();
        fdWriter.close();
        mdWriter.close();
        mpWriter.close();
        out.write(clOut.toString());
        out.write(fdOut.toString());
        out.write(mdOut.toString());
        out.write(mpOut.toString());
        out.close();
        try {
            clOut.close();
            fdOut.close();
            mdOut.close();
            mpOut.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Writes the given {@link ClassMapping} to the {@link JamWriter}'s
     * {@link PrintWriter}.
     *
     * @param classMapping The {@link ClassMapping} to write
     */
    protected void writeClassMapping(ClassMapping classMapping) {
        if (NOT_USELESS.test(classMapping)) {
            clWriter.format("CL %s %s\n",
                    classMapping.getFullObfuscatedName(), classMapping.getFullDeobfuscatedName());
        }

        classMapping.getInnerClassMappings().values().stream().filter(NOT_USELESS).forEach(this::writeClassMapping);
        classMapping.getFieldMappings().values().stream().filter(NOT_USELESS).forEach(this::writeFieldMapping);
        classMapping.getMethodMappings().values().forEach(this::writeMethodMapping);
    }

    /**
     * Writes the given {@link FieldMapping} to the {@link JamWriter}'s
     * {@link PrintWriter}.
     *
     * @param fieldMapping The {@link FieldMapping} to write
     */
    protected void writeFieldMapping(FieldMapping fieldMapping) {
        fdWriter.format("FD %s %s %s %s\n",
                fieldMapping.getParent().getFullObfuscatedName(),
                fieldMapping.getObfuscatedName(),
                fieldMapping.getObfuscatedType().toString(),
                fieldMapping.getDeobfuscatedName());
    }

    /**
     * Writes the given {@link MethodMapping} to the {@link JamWriter}'s
     * {@link PrintWriter}.
     *
     * @param mapping The {@link MethodMapping} to write
     */
    protected void writeMethodMapping(MethodMapping mapping) {
        if (!mapping.getObfuscatedName().equals(mapping.getDeobfuscatedName())) {
            mdWriter.format("MD %s %s %s %s\n",
                    mapping.getParent().getFullObfuscatedName(),
                    mapping.getObfuscatedName(),
                    mapping.getObfuscatedDescriptor(),
                    mapping.getDeobfuscatedName());
        }
        for (MethodParameterMapping pm : mapping.getParamMappings().values()) {
            mpWriter.format("MP %s %s %s %s %s\n",
                    pm.getParent().getParent().getFullObfuscatedName(),
                    pm.getParent().getObfuscatedName(),
                    pm.getParent().getObfuscatedDescriptor(),
                    pm.getIndex(),
                    pm.getDeobfuscatedName());
        }
    }
}
