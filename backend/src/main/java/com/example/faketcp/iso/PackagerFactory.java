package com.example.faketcp.iso;

import com.example.faketcp.config.PackagerProperties;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.janino.SimpleCompiler;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

@Component
public class PackagerFactory {
    public ISOBasePackager create(PackagerProperties properties) {
        try {
            if (properties == null) {
                throw new IllegalArgumentException("Packager config is empty");
            }
            PackagerProperties.PackagerType type = properties.getType();
            PackagerProperties.PackagerConfigMode configMode = properties.getConfigMode();
            if (type == null) {
                throw new IllegalArgumentException("Packager type is empty");
            }
            if (configMode == null) {
                throw new IllegalArgumentException("Packager configMode is empty");
            }

            if (type == PackagerProperties.PackagerType.XML) {
                if (configMode == PackagerProperties.PackagerConfigMode.XML_CONTENT) {
                    if (isBlank(properties.getContent())) {
                        throw new IllegalArgumentException("XML packager content is empty");
                    }
                    try (InputStream inputStream = new ByteArrayInputStream(properties.getContent().getBytes(StandardCharsets.UTF_8))) {
                        return new GenericPackager(inputStream);
                    }
                }
                if (configMode == PackagerProperties.PackagerConfigMode.XML_FILE) {
                    String location = properties.getFileName();
                    if (isBlank(location)) {
                        location = properties.getLocation();
                    }
                    if (isBlank(location)) {
                        throw new IllegalArgumentException("XML file packager requires fileName or location");
                    }
                    if (!location.startsWith("file:") && !location.startsWith("classpath:")) {
                        location = "classpath:packager/" + location;
                    }
                    try (InputStream inputStream = openLocation(location)) {
                        return new GenericPackager(inputStream);
                    }
                }
                throw new IllegalArgumentException("XML packager requires XML_CONTENT or XML_FILE mode");
            }

            if (type == PackagerProperties.PackagerType.CLASS) {
                if (configMode != PackagerProperties.PackagerConfigMode.CLASS_NAME) {
                    throw new IllegalArgumentException("CLASS packager requires CLASS_NAME mode");
                }
                if (isBlank(properties.getClassName())) {
                    throw new IllegalArgumentException("Class packager requires className");
                }
                return instantiate(Class.forName(properties.getClassName()), properties.getClassName());
            }

            if (type == PackagerProperties.PackagerType.CUSTOM) {
                if (configMode == PackagerProperties.PackagerConfigMode.JAVA_SOURCE) {
                    return compileSource(properties);
                }
                if (configMode == PackagerProperties.PackagerConfigMode.CLASS_NAME) {
                    if (isBlank(properties.getClassName())) {
                        throw new IllegalArgumentException("Custom class packager requires className");
                    }
                    return instantiate(Class.forName(properties.getClassName()), properties.getClassName());
                }
                throw new IllegalArgumentException("CUSTOM packager requires JAVA_SOURCE or CLASS_NAME mode");
            }

            throw new IllegalArgumentException("Unsupported packager type: " + type);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new IllegalStateException("Failed to create ISO8583 packager: " + message, e);
        }
    }

    private ISOBasePackager compileSource(PackagerProperties properties) throws Exception {
        if (isBlank(properties.getContent())) {
            throw new IllegalArgumentException("Custom Java source is empty");
        }
        String className = extractClassName(properties.getContent());
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setParentClassLoader(Thread.currentThread().getContextClassLoader());
        compiler.cook(properties.getContent());
        return instantiate(compiler.getClassLoader().loadClass(className), className);
    }

    private ISOBasePackager instantiate(Class<?> packagerClass, String className) throws Exception {
        Object instance = packagerClass.getDeclaredConstructor().newInstance();
        if (!(instance instanceof ISOBasePackager)) {
            throw new IllegalArgumentException(className + " must extend ISOBasePackager");
        }
        return (ISOBasePackager) instance;
    }

    private String extractClassName(String source) {
        Matcher packageMatcher = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;").matcher(source);
        Matcher classMatcher = Pattern.compile("\\bclass\\s+(\\w+)").matcher(source);
        if (!classMatcher.find()) {
            throw new IllegalArgumentException("Cannot find class name in Java source");
        }
        String simpleName = classMatcher.group(1);
        if (packageMatcher.find()) {
            return packageMatcher.group(1) + "." + simpleName;
        }
        return simpleName;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private InputStream openLocation(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            return new ClassPathResource(location.substring("classpath:".length())).getInputStream();
        }
        return new FileInputStream(ResourceUtils.getFile(location));
    }
}
