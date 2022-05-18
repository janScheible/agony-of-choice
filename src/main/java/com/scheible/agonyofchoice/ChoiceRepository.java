package com.scheible.agonyofchoice;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

/**
 *
 */
@Repository
public class ChoiceRepository {

	@Value("${agony-of-choice.choices-file-location:classpath:choices.txt}")
	private String choicesLocation;
	
	private final ResourceLoader resourceLoader;

	@Autowired
	public ChoiceRepository(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public ChoiceRepository(String choicesFileName, ResourceLoader resourceLoader) {
		this.choicesLocation = choicesFileName;
		this.resourceLoader = resourceLoader;
	}

	public List<String> load() {
		try (InputStream input = resourceLoader.getResource(choicesLocation).getInputStream()) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
