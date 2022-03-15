///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS commons-io:commons-io:2.11.0

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "histogram", mixinStandardHelpOptions = true, version = "histogram 0.1", description = "histogram made with jbang")
class Histogram implements Callable<Integer> {

    private static Set<String> STOP_WORDS;
    private static Set<String> TRASH;

    @Option(names = { "-p", "--path" }, description = "Path for read code")
    private String path;

    @Option(names = { "-ext", "--extensions" }, description = "Path for read code")
    private String extensions;

    @Option(names = { "-s", "--size" }, description = "Number of words to be printed", defaultValue = "15")
    private int size;

    public static void main(String... args) {
        STOP_WORDS = readLines(Paths.get("stopwords-pt-BR.txt").toFile()).collect(Collectors.toSet());
        TRASH = readLines(Paths.get("trash.txt").toFile()).collect(Collectors.toSet());
        int exitCode = new CommandLine(new Histogram()).execute(args);
        System.exit(exitCode);
    }

    private static Stream<String> readLines(File file) {
        try {
            return IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8)
                    .stream();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Cannot find file: " + file.getAbsolutePath(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read file: " + file.getAbsolutePath(), ex);
        }
    }

    @Override
    public Integer call() throws Exception {
        var histogram = listAll(Paths.get(path).toFile())
                .flatMap(Histogram::readLines)
                .filter(line -> !line.trim().isBlank())
                .flatMap(line -> Stream.of(line.trim().toLowerCase().split("\\s")))
                .map(word -> word.replaceAll("[,\\.;]$", ""))
                .filter(line -> !line.trim().isBlank())
                .filter(Predicate.not(Histogram::isStopWord))
                .filter(Predicate.not(Histogram::isTrash))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        histogram.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(size)
                .forEachOrdered(entry -> {
                    System.out.println(entry);
                });

        return 0;
    }

    private static boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }

    private static boolean isTrash(String word) {
        return TRASH.contains(word);
    }

    private Stream<File> listAll(File startPath) {
        if (startPath.isDirectory()) {
            return Stream.of(startPath.listFiles())
                    .flatMap(this::listAll);
        } else {
            if (startPath.getName().endsWith(extensions)) {
                return Stream.of(startPath);
            } else {
                return Stream.empty();
            }
        }
    }
}
