package com.scheible.agonyofchoice;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ChoiceService {

	public record OptionId(String key) implements Comparable<OptionId> {

		private static final Pattern ONLY_DIGITS = Pattern.compile("[^0-9]");

		@Override
		public int compareTo(OptionId other) {
			var numberKey = extractNumber(key);
			var otherNumberKey = extractNumber(other.key);

			if (numberKey != null && otherNumberKey != null) {
				return Integer.compare(numberKey, otherNumberKey);
			} else {
				return key.compareTo(other.key);
			}
		}

		private static Integer extractNumber(String value) {
			var valueOnlyDigits = ONLY_DIGITS.matcher(value).replaceAll("");
			return valueOnlyDigits.isEmpty() ? null : Integer.parseInt(valueOnlyDigits);
		}
	}

	public record Prerequisite(String text) {

	}

	public enum ArgumentType {
		PRO, CON
	}

	public record Argument(String text, ArgumentType type, Set<OptionId> onlyWiths) {

	}

	public record Option(OptionId id, String title, Set<Prerequisite> prerequisites, Set<OptionId> contradicts,
			Set<Argument> arguments) {

	}

	public record Choice(Option a, Option b) {

	}

	public record Choices(String title, List<Choice> choices) {

	}

	public record Contradiction(OptionId a, OptionId b) {

		public static Contradiction ofSorted(OptionId a, OptionId b) {
			return a.compareTo(b) < 0 ? new Contradiction(a, b) : new Contradiction(b, a);
		}
	}

	public record OptionPrerequisite(OptionId option, Prerequisite prerequisite) {

	}

	public record OptionArgument(OptionId option, Argument argument) {

	}

	public record Result(Set<OptionId> optionIds, Set<Contradiction> contradictions,
			Set<OptionPrerequisite> optionPrerequisites, Set<OptionArgument> optionArguments) {

	}

	public Choices parseChoices(List<String> lines) {
		var choices = new ArrayList<Choice>();
		var options = new ArrayList<Option>();

		String title = null;

		for (int i = 0; i < lines.size(); i++) {
			var line = lines.get(i).trim();

			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			} else if (title == null && !line.isEmpty()) {
				title = line;
			} else if (line.startsWith("[")) {
				for (int j = i + 1; j < lines.size(); j++) {
					if (lines.get(j).startsWith("[") || j == lines.size() - 1) {
						options.add(parseOption(lines.subList(i, j)));
						i = j - 1;
						break;
					}
				}
			}
			if (options.size() == 2) {
				choices.add(new Choice(options.get(0), options.get(1)));
				options.clear();
			}
		}

		return new Choices(title, choices);
	}

	private Option parseOption(List<String> lines) {
		var id = new OptionId(lines.get(0).substring(0, lines.get(0).indexOf(']') + 1));
		var title = lines.get(0).substring(lines.get(0).indexOf(']') + 1).trim();

		var prerequisites = new HashSet<Prerequisite>();
		var contradicts = new HashSet<OptionId>();
		var arguments = new HashSet<Argument>();

		for (int i = 0; i < lines.size(); i++) {
			var line = lines.get(i).trim();

			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			} else if (line.startsWith("-- prerequisites")) {
				for (int j = i + 1; j < lines.size(); j++) {
					line = lines.get(j).trim();

					if (!line.startsWith("---")) {
						break;
					} else {
						prerequisites.add(new Prerequisite(line.replaceFirst("---", "").trim()));
					}
				}
			} else if (line.startsWith("-- contradicts")) {
				contradicts.addAll(parseOptionIdList(line));
			} else if (line.startsWith("-- pros") || line.startsWith("-- cons")) {
				var type = line.startsWith("-- pros") ? ArgumentType.PRO : ArgumentType.CON;

				for (int j = i + 1; j < lines.size(); j++) {
					line = lines.get(j).trim();

					if (!line.startsWith("---")) {
						break;
					} else {
						arguments.add(new Argument((line.contains("]}") ? line.substring(line.indexOf("]}") + 2) : line)
								.replaceFirst("---", "").trim(), type, parseOptionIdList(line)));
					}
				}
			}
		}

		return new Option(id, title, prerequisites, contradicts, arguments);
	}

	static Set<OptionId> parseOptionIdList(String text) {
		var result = new HashSet<OptionId>();

		var from = text.indexOf('{');
		var to = text.indexOf('}');
		if (from >= 0 && to >= 0 && to > 0) {
			var listPart = text.substring(from + 1, to);
			for (String idValue : listPart.split(",")) {
				if (!idValue.isBlank()) {
					result.add(new OptionId(idValue.trim()));
				}
			}
		}

		return result;
	}

	public Result choose(List<Choice> choices, Set<OptionId> optionIds) {
		var optionIdMap = choices.stream().flatMap(c -> Stream.of(new SimpleImmutableEntry<>(c.a.id, c.a),
				new SimpleImmutableEntry<>(c.b.id, c.b))).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		var contradictions = new HashSet<Contradiction>();
		var prerequisites = new HashSet<OptionPrerequisite>();
		var arguments = new HashSet<OptionArgument>();

		for (OptionId optionId : optionIds) {
			var option = optionIdMap.get(optionId);

			for (OptionId contradictingId : option.contradicts) {
				if (optionIds.contains(contradictingId)) {
					// by sorting the optionIds we avoid having 'a -> b' and 'b -> a' both as a result
					contradictions.add(Contradiction.ofSorted(optionId, contradictingId));
				}
			}

			for (Prerequisite prerequisite : option.prerequisites) {
				prerequisites.add(new OptionPrerequisite(optionId, prerequisite));
			}

			for (Argument argument : option.arguments) {
				if (argument.onlyWiths.isEmpty()) {
					arguments.add(new OptionArgument(optionId, argument));
				} else {
					for (OptionId argumentOnlyWidth : argument.onlyWiths) {
						if (optionIds.contains(argumentOnlyWidth)) {
							arguments.add(new OptionArgument(optionId, argument));
						}
					}
				}
			}
		}

		return new Result(optionIds, contradictions, prerequisites, arguments);
	}
}
