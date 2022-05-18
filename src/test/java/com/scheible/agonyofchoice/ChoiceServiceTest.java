package com.scheible.agonyofchoice;

import com.scheible.agonyofchoice.ChoiceService.Argument;
import static com.scheible.agonyofchoice.ChoiceService.ArgumentType.CON;
import static com.scheible.agonyofchoice.ChoiceService.ArgumentType.PRO;
import com.scheible.agonyofchoice.ChoiceService.Contradiction;
import com.scheible.agonyofchoice.ChoiceService.OptionArgument;
import com.scheible.agonyofchoice.ChoiceService.OptionId;
import com.scheible.agonyofchoice.ChoiceService.OptionPrerequisite;
import com.scheible.agonyofchoice.ChoiceService.Prerequisite;
import java.io.IOException;
import static java.util.Collections.emptySet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

/**
 *
 */
public class ChoiceServiceTest {

	private final ChoiceRepository choiceRepository = new ChoiceRepository("test-choices.txt", new DefaultResourceLoader());
	private final ChoiceService choiceService = new ChoiceService();

	@Test
	void testContradictionOfSorted() {
		assertThat(Contradiction.ofSorted(new OptionId("a"), new OptionId("a")))
				.isEqualTo(new Contradiction(new OptionId("a"), new OptionId("a")));
		assertThat(Contradiction.ofSorted(new OptionId("a"), new OptionId("b")))
				.isEqualTo(new Contradiction(new OptionId("a"), new OptionId("b")));
		assertThat(Contradiction.ofSorted(new OptionId("b"), new OptionId("a")))
				.isEqualTo(new Contradiction(new OptionId("a"), new OptionId("b")));
	}

	@Test
	void testParseChoices() throws IOException {
		var choices = choiceService.parseChoices(choiceRepository.load());
		
		assertThat(choices.title()).isEqualTo("title");

		assertThat(choices.choices().get(0).a().id()).isEqualTo(new OptionId("[OPT1]"));
		assertThat(choices.choices().get(0).b().id()).isEqualTo(new OptionId("[OPT2]"));
		assertThat(choices.choices().get(1).a().id()).isEqualTo(new OptionId("[OPT3]"));
		assertThat(choices.choices().get(1).b().id()).isEqualTo(new OptionId("[OPT4]"));
		
		assertThat(choices.choices().get(0).a().contradicts()).containsOnly(new OptionId("[OPT4]"));
		assertThat(choices.choices().get(0).b().contradicts()).isEmpty();
		assertThat(choices.choices().get(1).a().contradicts()).isEmpty();
		assertThat(choices.choices().get(1).b().contradicts()).isEmpty();
		
		assertThat(choices.choices().get(0).a().prerequisites())
				.containsOnly(new Prerequisite("prereq 1.1"), new Prerequisite("prereq 1.2"));
		assertThat(choices.choices().get(0).b().prerequisites()).isEmpty();
		assertThat(choices.choices().get(1).a().prerequisites()).isEmpty();
		assertThat(choices.choices().get(1).b().prerequisites()).isEmpty();
		
		assertThat(choices.choices().get(0).a().arguments()).containsOnly(new Argument("pro 1.1", PRO, emptySet()),
				new Argument("con 1.1", CON, emptySet()), new Argument("con 1.2", CON, Set.of(new OptionId("[OPT3]"))));
		assertThat(choices.choices().get(0).b().arguments()).isEmpty();
		assertThat(choices.choices().get(1).a().arguments()).isEmpty();
		assertThat(choices.choices().get(1).b().arguments()).containsOnly(new Argument("pro 4.1", PRO, emptySet()),
				new Argument("con 4.1", CON, emptySet()));
	}

	@Test
	void testOptionIdListParsing() {
		assertThat(ChoiceService.parseOptionIdList("{}")).containsOnly();
		assertThat(ChoiceService.parseOptionIdList("{[CHOICE2]}")).containsOnly(new OptionId("[CHOICE2]"));
		assertThat(ChoiceService.parseOptionIdList("{[CHOICE2],[CHOICE3]}"))
				.containsOnly(new OptionId("[CHOICE2]"), new OptionId("[CHOICE3]"));
	}

	@Test
	void testChoose() throws IOException {
		var choices = choiceService.parseChoices(choiceRepository.load()).choices();
		var result = choiceService.choose(choices, Set.of(choices.get(0).a().id(), choices.get(1).b().id()));

		assertThat(result.contradictions()).containsOnly(new Contradiction(new OptionId("[OPT1]"), new OptionId("[OPT4]")));
		assertThat(result.optionPrerequisites().stream().map(OptionPrerequisite::option))
				.containsExactlyInAnyOrder(new OptionId("[OPT1]"), new OptionId("[OPT1]"));
		assertThat(result.optionArguments().stream().map(OptionArgument::option))
				.containsExactlyInAnyOrder(new OptionId("[OPT1]"), new OptionId("[OPT1]"),
						new OptionId("[OPT4]"), new OptionId("[OPT4]"));
	}
}
