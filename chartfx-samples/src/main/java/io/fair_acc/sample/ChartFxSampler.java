package io.fair_acc.sample;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import fxsampler.FXSampler;

public class ChartFxSampler {
    private static boolean debug = Optional.ofNullable(System.getenv("chartfx.debug"))
                                           .map(Boolean::parseBoolean)
                                           .orElse(false);

    public static void main(String[] args) {
        if (debug) {
            // Debug output for checking classpath
            System.out.println("\nJars on ClassPath:");
            String classpath = System.getProperty("java.class.path");
            Arrays.stream(classpath.split(File.pathSeparator))
                    .map(String::toString)
                    .filter(str -> str.endsWith(".jar"))
                    .map(str -> "* " + str.substring(str.lastIndexOf(File.separatorChar) + 1))
                    .sorted()
                    .forEach(System.out::println);

            // The FX Sampler only searches for jars/classes at and below the working directory
            System.out.println("\nworkingDir = " + Path.of(".").toAbsolutePath().normalize() + "\n");
        }

        FXSampler.main(args);
    }
}
