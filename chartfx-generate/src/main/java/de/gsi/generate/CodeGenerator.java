package de.gsi.generate;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

/**
 * Proof of concept code generator that gets called by the maven exec plugin during the generate
 * sources phase. It checks the source files for code gen templates by assuming some naming convention,
 * and then does search/replace based on sections within the source file. This allows multiple
 * output types to be generated into a single class.
 *
 * Currently the generated class inherits from the base class, but I'm not sure that's necessary.
 * We could just generate a full copy of the base class and then add sections within the template
 * area. TBD.
 *
 * @author ennerf
 */
public class CodeGenerator {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            throw new Exception("Expected [Input Sources] [Output Sources]");
        }

        final Path input = Paths.get(args[0]);
        final Path output = Paths.get(args[1]);

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**GenBase.java");
        final List<Path> sources = Files.walk(input)
                .filter(matcher::matches)
                .collect(Collectors.toList());

        StringBuilder debug = new StringBuilder();
        for (Path source : sources) {

            // Create output package
            Path relativePath = input.relativize(source);
            Path outputDirectory = output.resolve(relativePath.getParent());
            Files.createDirectories(outputDirectory);

            // Find name to be generated
            String sourceClassName = removeTail(source.getFileName().toString(), ".java");
            String outputClassName = removeTail(sourceClassName, "GenBase");

            // Generate output file
            Path dest = outputDirectory.resolve(outputClassName + ".java");
            final List<String> sourceContent = Files.readAllLines(source);
            try (Writer writer = Files.newBufferedWriter(dest, CREATE, WRITE, TRUNCATE_EXISTING)) {

                // Write minimum class layout. Maybe use JavaPoet?
                writer.write("// This file has been generated automatically. Do not modify!\n");
                writer.write("package " + relativePath.getParent().toString().replaceAll("(\\\\|/)", ".") + ";\n");
                generateImports(sourceContent, writer);
                writer.write(String.format("public class %s extends %s {\n", outputClassName, sourceClassName));
                generateContent(sourceContent, writer);
                writer.write("\n}");
            }

        }

    }

    private static void generateImports(List<String> source, Writer writer) throws IOException {
        List<String> lines = getSection(source,
                "//// === BEGIN IMPORTS ===",
                "//// === END IMPORTS ===");
        writeLines(lines, writer, line -> line);
    }

    /**
     * Generate some sort of code, e.g., search/replace within some template boundary
     */
    private static void generateContent(List<String> source, Writer writer) throws IOException {
        // Limit file contents to the "template" section within the source file
        List<String> lines = getSection(source,
                "//// === BEGIN TEMPLATE ===",
                "//// === END TEMPLATE ===");

        writer.write("// <float>");
        writeLines(lines, writer, line -> line
                .replaceAll("double", "float")
                .replaceAll("Double", "Float"));

        writer.write("// <short>");
        writeLines(lines, writer, line -> line
                .replaceAll("double", "short")
                .replaceAll("Double", "Short"));

        writer.write("// <int>");
        writeLines(lines, writer, line -> line
                .replaceAll("double", "int")
                .replaceAll("Double", "Int"));

        writer.write("// <long>");
        writeLines(lines, writer, line -> line
                .replaceAll("double", "long")
                .replaceAll("Double", "Long"));

    }

    private static void writeLines(List<String> lines, Writer writer, Function<String, String> converter) throws IOException {
        for (String line : lines) {
            writer.write(converter.apply(line));
            writer.write("\n");
        }
    }

    private static List<String> getSection(List<String> source, String beginIdentifier, String endIdentifier) throws IOException {
        int startIndex = source.indexOf(beginIdentifier);
        int endIndex = source.lastIndexOf(endIdentifier);
        if (startIndex < 0) {
            throw new IOException("File does not contain identifier: " + beginIdentifier);
        } else if (endIndex < 0) {
            throw new IOException("File does not contain identifier: " + endIdentifier);
        } else {
            return source.subList(startIndex + 1, endIndex);
        }
    }

    private static String removeTail(String string, String tail) {
        if (!string.endsWith(tail)) throw new IllegalStateException(string + " does not end with " + tail);
        return string.substring(0, string.length() - tail.length());
    }

}
