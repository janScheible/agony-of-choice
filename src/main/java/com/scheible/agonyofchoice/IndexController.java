package com.scheible.agonyofchoice;

import com.scheible.agonyofchoice.ChoiceService.Argument;
import com.scheible.agonyofchoice.ChoiceService.ArgumentType;
import com.scheible.agonyofchoice.ChoiceService.Choice;
import com.scheible.agonyofchoice.ChoiceService.Option;
import com.scheible.agonyofchoice.ChoiceService.OptionArgument;
import com.scheible.agonyofchoice.ChoiceService.OptionId;
import com.scheible.agonyofchoice.ChoiceService.OptionPrerequisite;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 */
@Controller
public class IndexController {
	
	private static final int CHOICE_COLUMNS = 2;

	private final ChoiceRepository choiceRepository;
	private final ChoiceService choiceService;

	public IndexController(ChoiceRepository choiceRepository, ChoiceService choiceService) {
		this.choiceRepository = choiceRepository;
		this.choiceService = choiceService;
	}

	@GetMapping("/index.html")
	public ModelAndView indexHtml(Model model, @RequestParam Map<String, String> allParams) {
		return indexRoot(model, allParams);
	}

	@GetMapping("/")
	public ModelAndView indexRoot(Model model, @RequestParam Map<String, String> allParams) {
		var choices = choiceService.parseChoices(choiceRepository.load());

		model.addAttribute("title", choices.title());
		model.addAttribute("choicesRows", toChoicesRows(choices.choices()));

		Function<Option, String> optionIdGetter = option -> option.id().key().replace("[", "").replace("]", "");
		model.addAttribute("optionIdGetter", optionIdGetter);
		Function<Choice, String> choiceIdGetter = choice -> optionIdGetter.apply(choice.a())
				+ "-" + optionIdGetter.apply(choice.b());
		model.addAttribute("choiceIdGetter", choiceIdGetter);

		Function<Argument, Boolean> proArgumentChecker = argument -> argument.type() == ArgumentType.PRO;
		model.addAttribute("proArgumentChecker", proArgumentChecker);
		
		var selectedOptions = allParams.entrySet().stream().filter(param -> param.getKey().contains("-")).map(Entry::getValue)
				.map(paramValue -> new OptionId("[" + paramValue + "]")).collect(Collectors.toSet());

		Function<Option, Boolean> isSelected = option -> selectedOptions.contains(option.id());
		model.addAttribute("isSelected", isSelected);

		var result = choiceService.choose(choices.choices(), selectedOptions);
		model.addAttribute("result", result);

		Function<Set<OptionPrerequisite>, List<OptionPrerequisite>> optionPrerequisitesSorter = optionPrerequisitesSet
				-> optionPrerequisitesSet.stream().sorted(Comparator.comparing(OptionPrerequisite::option)).toList();
		model.addAttribute("optionPrerequisitesSorter", optionPrerequisitesSorter);
		
		Function<Set<OptionArgument>, List<OptionArgument>> optionArgumentsSorter = optionArgumentsSet
				-> optionArgumentsSet.stream().sorted(Comparator.comparing(OptionArgument::option)).toList();
		model.addAttribute("optionArgumentsSorter", optionArgumentsSorter);		

		return new ModelAndView("index", model.asMap());
	}
	
	private static List<List<Choice>> toChoicesRows(List<Choice> choices) {
		var choicesRows = new ArrayList<List<Choice>>();

		for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
			int rowStartIndex = choiceIndex;
			var row = new ArrayList<Choice>();

			for (; choiceIndex < Math.min(rowStartIndex + CHOICE_COLUMNS, choices.size()); choiceIndex++) {
				row.add(choices.get(choiceIndex));
			}
			choiceIndex--;

			// fill up the last row with nulls
			while (row.size() != CHOICE_COLUMNS) {
				row.add(null);
			}

			choicesRows.add(row);
		}

		return choicesRows;
	}
}
