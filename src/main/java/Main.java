import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {

        List<String> allLines = Files.readAllLines(Paths.get("src/main/resources/input.txt"), StandardCharsets.UTF_8);

        Scanner sc= new Scanner(System.in);

        boolean correctSizes = false;
        int firstGroupSize = 0;
        int secondGroupSize = 0;

        while (!correctSizes){
            firstGroupSize = sc.nextInt();
            secondGroupSize = sc.nextInt();
            if(firstGroupSize + secondGroupSize <= allLines.size()) {
                correctSizes = true;
            } else {
                System.out.println("В input.txt всего " + allLines.size() + " строк. Укажите корректный размер групп");
            }
        }

        List<String> firstGroup = allLines.subList(0, firstGroupSize);
        List<String> secondGroup = allLines.subList(firstGroupSize, firstGroupSize + secondGroupSize);

        //Лемматизация
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<List<String>> firstGroupNormalFormsLists = getNormalForms(luceneMorph, firstGroup);
        ArrayList<List<String>> secondGroupNormalFormsLists = getNormalForms(luceneMorph, secondGroup);

        //Сопоставление групп
        if (firstGroupSize >= secondGroupSize)
            matchLines(firstGroup, secondGroup, firstGroupNormalFormsLists, secondGroupNormalFormsLists, false);
        else
            matchLines(secondGroup, firstGroup, secondGroupNormalFormsLists, firstGroupNormalFormsLists, true);

    }

    private static void matchLines(List<String> firstGroup, List<String> secondGroup, ArrayList<List<String>> firstGroupNormalFormsLists, ArrayList<List<String>> secondGroupNormalFormsLists, boolean switchWords) throws IOException {

        List<String> firstGroupUnmatched = new ArrayList<>();
        List<String> secondGroupMatched = new ArrayList<>();
        LinkedHashMap<String, String> finalHashMap = new LinkedHashMap<>();

        for (int i = 0; i < firstGroupNormalFormsLists.size(); i++) {

            List<String> firstGroupWords = firstGroupNormalFormsLists.get(i);
            int numberOfMatches = 0;
            int maxNumberOfMatches = 0;
            String match = "?";

            for (int j = 0; j < secondGroupNormalFormsLists.size(); j++) {
                List<String> secondGroupWords = secondGroupNormalFormsLists.get(j);

                for (String firstGroupWord: firstGroupWords) {
                    for (String secondGroupWord: secondGroupWords) {

                        if ((firstGroupWord.equals(secondGroupWord)) && (!firstGroupUnmatched.contains(j))) {
                            numberOfMatches++;
                            if (maxNumberOfMatches < numberOfMatches) {
                                maxNumberOfMatches = numberOfMatches;
                                match = secondGroup.get(j);
                            }
                        }
                    }
                }

            }

            if (switchWords && !match.equals("?")) {
                secondGroupMatched.add(match);
                finalHashMap.put(match, firstGroup.get(i));
            }
            else {
                finalHashMap.put(firstGroup.get(i), match);
                if (!match.equals("?"))
                    secondGroupMatched.add(match);
                else
                    firstGroupUnmatched.add(firstGroup.get(i));
            }
        }

        //Сопоставить, если есть слова без соответствий в 1й и 2й группах
        for (String str1: firstGroupUnmatched) {
            for (String str2: secondGroup) {
                if (!secondGroupMatched.contains(str2)){
                    secondGroupMatched.add(str2);
                    finalHashMap.put(str1, str2);
                    break;
                }
            }
        }

        FileWriter writer = new FileWriter("src/main/resources/output.txt", false);
        for (String str: finalHashMap.keySet()) {
            writer.write(str + " : " + finalHashMap.get(str) + System.lineSeparator());
        }
        writer.flush();

    }

    private static ArrayList<List<String>> getNormalForms(LuceneMorphology luceneMorph, List<String> group) {

        String regex = "[а-яёА-ЯЁ]+";
        Pattern pattern = Pattern.compile(regex);

        ArrayList<List<String>> normalFormsLists = new ArrayList<>();

        for (String line: group) {
            List<String> words = List.of(line.split(" "));
            List<String> normalForms = new ArrayList<>();
            for (String word: words) {
                Matcher matcher = pattern.matcher(word);
                if (matcher.matches()) {
                    normalForms = Stream.concat(normalForms.stream(), luceneMorph.getNormalForms(word.toLowerCase()).stream()).toList();
                }
            }

            normalFormsLists.add(normalForms);
        }

        return normalFormsLists;
    }

}
