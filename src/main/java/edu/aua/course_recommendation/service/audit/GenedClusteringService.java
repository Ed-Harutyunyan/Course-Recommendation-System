package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.NeededCluster;
import edu.aua.course_recommendation.service.course.CourseService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class GenedClusteringService {

    private final CourseService courseService;

    private static final Set<Integer> AH_THEMES = Set.of(1, 2, 3);
    private static final Set<Integer> SS_THEMES = Set.of(4, 5, 6);
    private static final Set<Integer> QS_THEMES = Set.of(7, 8, 9);

    public Set<NeededCluster> findNeededClusters(List<Course> completedCourses) {
        Set<NeededCluster> needed = new HashSet<>();

        boolean canFormAH = false;
        for (int theme : AH_THEMES) {
            if (canFormTriple(completedCourses, theme)) {
                canFormAH = true;
                break;
            }
        }
        if (!canFormAH) {
            needed.addAll(analyzeThemeShortage(completedCourses, AH_THEMES));
        }

        boolean canFormSS = false;
        for (int theme : SS_THEMES) {
            if (canFormTriple(completedCourses, theme)) {
                canFormSS = true;
                break;
            }
        }
        if (!canFormSS) {
            needed.addAll(analyzeThemeShortage(completedCourses, SS_THEMES));
        }

        boolean canFormQS = false;
        for (int theme : QS_THEMES) {
            if (canFormTriple(completedCourses, theme)) {
                canFormQS = true;
                break;
            }
        }
        if (!canFormQS) {
            needed.addAll(analyzeThemeShortage(completedCourses, QS_THEMES));
        }

        return needed;
    }

    public List<NeededCluster> getGenedClusters(List<Course> courses) {
        List<NeededCluster> clusters = new ArrayList<>();

        for (int theme = 1; theme <= 9; theme++) {
            int finalTheme = theme;
            List<Course> themeSpecificCourses = courses.stream()
                    .filter(c -> c.getThemes().contains(finalTheme))
                    .toList();

            int lowerCount = (int) themeSpecificCourses.stream()
                    .map(Course::getCode)
                    .filter(courseService::isLowerDivision)
                    .count();

            int upperCount = (int) themeSpecificCourses.stream()
                    .map(Course::getCode)
                    .filter(courseService::isUpperDivision)
                    .count();

            int totalCount = lowerCount + upperCount;

            int missingLower = lowerCount < 1 ? 1 - lowerCount : 0;
            int missingUpper = upperCount < 1 ? 1 - upperCount : 0;
            int missingTotal = Math.max(0, 3 - totalCount);

            clusters.add(new NeededCluster(theme, missingLower, missingUpper, missingTotal));
        }

        return clusters;
    }

    public List<String> buildMissingGenEdCodes(
            Set<NeededCluster> neededClusters,
            Set<String> genEdEligibleCodes,
            List<Course> genEdCompleted
    ) {
        List<String> missing = new ArrayList<>();

        Set<String> completedCodes = genEdCompleted.stream()
                .map(Course::getCode)
                .collect(Collectors.toSet());

        for (NeededCluster needed : neededClusters) {
            int theme = needed.getTheme();
            int needLower = needed.getMissingLower();
            int needUpper = needed.getMissingUpper();
            int needTotal = needed.getMissingTotal();

            Set<String> themeCodes = findCodesForTheme(theme, genEdEligibleCodes);

            if (needLower > 0) {
                Set<String> potentialLower = themeCodes.stream()
                        .filter(courseService::isLowerDivision)
                        .filter(code -> !completedCodes.contains(code))
                        .collect(Collectors.toSet());
                missing.addAll(potentialLower);
            }

            if (needUpper > 0) {
                Set<String> potentialUpper = themeCodes.stream()
                        .filter(courseService::isUpperDivision)
                        .filter(code -> !completedCodes.contains(code))
                        .collect(Collectors.toSet());
                missing.addAll(potentialUpper);
            }

            if (needTotal > 0) {
                Set<String> anyRemaining = themeCodes.stream()
                        .filter(code -> !completedCodes.contains(code))
                        .collect(Collectors.toSet());
                missing.addAll(anyRemaining);
            }
        }

        return missing;
    }

    private Set<String> findCodesForTheme(int theme, Set<String> genEdEligibleCodes) {
        return courseService.getAllCourses().stream()
                .filter(c -> c.getThemes().contains(theme))
                .map(Course::getCode)
                .filter(genEdEligibleCodes::contains)
                .collect(Collectors.toSet());
    }

    private Set<NeededCluster> analyzeThemeShortage(List<Course> courses, Set<Integer> sectorThemes) {
        Set<NeededCluster> needed = new HashSet<>();

        for (int theme : sectorThemes) {
            if (canFormTriple(courses, theme)) {
                continue;
            }
            needed.add(analyzeSingleTheme(courses, theme));
        }

        return needed;
    }

    private NeededCluster analyzeSingleTheme(List<Course> courses, int theme) {

        List<Course> filtered = courses.stream()
                .filter(c -> c.getThemes().contains(theme))
                .toList();

        int lowerCount = 0, upperCount = 0;
        for (Course c : filtered) {
            if (courseService.isLowerDivision(c.getCode())) lowerCount++;
            else if (courseService.isUpperDivision(c.getCode())) upperCount++;
        }

        NeededCluster needed = new NeededCluster();
        needed.setTheme(theme);

        if (lowerCount < 1) {
            needed.setMissingLower(1 - lowerCount);
        }
        if (upperCount < 1) {
            needed.setMissingUpper(1 - upperCount);
        }
        int total = lowerCount + upperCount;
        if (total < 3) {
            needed.setMissingTotal(3 - total);
        }

        return needed;
    }

    public boolean isGenEdRequirementMet(List<Course> completedCourses) {
        List<ClusterSolution> solutions = findPossibleClusterCombinations(completedCourses, 1);
        return !solutions.isEmpty();
    }

    public List<ClusterSolution> findPossibleClusterCombinations(List<Course> completedCourses, int maxSolutions) {
        List<ClusterSolution> solutions = new ArrayList<>();
        backtrackAH(new ArrayList<>(completedCourses), new ArrayList<>(), solutions, maxSolutions);
        return solutions;
    }

    // =============== BACKTRACKING FOR AH ===============
    private void backtrackAH(
            List<Course> available,
            List<ClusterChoice> chosen,
            List<ClusterSolution> solutions,
            int maxSolutions
    ) {
        if (solutions.size() >= maxSolutions) return;

        for (int theme : AH_THEMES) {
            List<List<Course>> validTriples = findValidTriples(available, theme);

            for (List<Course> triple : validTriples) {
                ClusterChoice ahChoice = new ClusterChoice("AH", theme, triple);

                List<ClusterChoice> newChosen = new ArrayList<>(chosen);
                newChosen.add(ahChoice);

                List<Course> newAvailable = new ArrayList<>(available);
                newAvailable.removeAll(triple);

                backtrackSS(newAvailable, newChosen, solutions, maxSolutions);
                if (solutions.size() >= maxSolutions) return;
            }
        }
    }

    // =============== BACKTRACKING FOR SS ===============
    private void backtrackSS(
            List<Course> available,
            List<ClusterChoice> chosen,
            List<ClusterSolution> solutions,
            int maxSolutions
    ) {
        if (solutions.size() >= maxSolutions) return;

        for (int theme : SS_THEMES) {
            List<List<Course>> validTriples = findValidTriples(available, theme);

            for (List<Course> triple : validTriples) {
                ClusterChoice ssChoice = new ClusterChoice("SS", theme, triple);

                List<ClusterChoice> newChosen = new ArrayList<>(chosen);
                newChosen.add(ssChoice);

                List<Course> newAvailable = new ArrayList<>(available);
                newAvailable.removeAll(triple);

                backtrackQS(newAvailable, newChosen, solutions, maxSolutions);
                if (solutions.size() >= maxSolutions) return;
            }
        }
    }

    // =============== BACKTRACKING FOR QS ===============
    private void backtrackQS(
            List<Course> available,
            List<ClusterChoice> chosen,
            List<ClusterSolution> solutions,
            int maxSolutions
    ) {
        if (solutions.size() >= maxSolutions) return;

        for (int theme : QS_THEMES) {
            List<List<Course>> validTriples = findValidTriples(available, theme);

            for (List<Course> triple : validTriples) {
                ClusterChoice qsChoice = new ClusterChoice("QS", theme, triple);

                List<ClusterChoice> newChosen = new ArrayList<>(chosen);
                newChosen.add(qsChoice);

                solutions.add(new ClusterSolution(newChosen));
                if (solutions.size() >= maxSolutions) return;
            }
        }
    }

    // =============== FIND VALID TRIPLES ===============
    private List<List<Course>> findValidTriples(List<Course> available, int theme) {

        List<Course> filtered = available.stream()
                .filter(c -> c.getThemes().contains(theme))
                .toList();

        List<List<Course>> combos = combinationsOf3(filtered);
        List<List<Course>> valid = new ArrayList<>();
        for (List<Course> triple : combos) {
            if (hasAtLeastOneLower(triple) && hasAtLeastOneUpper(triple)) {
                valid.add(triple);
            }
        }
        return valid;
    }

    private boolean canFormTriple(List<Course> courses, int theme) {
        List<List<Course>> valid = findValidTriples(courses, theme);
        return !valid.isEmpty();
    }

    private List<List<Course>> combinationsOf3(List<Course> list) {
        List<List<Course>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i+1; j < list.size(); j++) {
                for (int k = j+1; k < list.size(); k++) {
                    result.add(List.of(list.get(i), list.get(j), list.get(k)));
                }
            }
        }
        return result;
    }

    private boolean hasAtLeastOneLower(List<Course> triple) {
        return triple.stream().map(Course::getCode).anyMatch(courseService::isLowerDivision);
    }


    private boolean hasAtLeastOneUpper(List<Course> triple) {
        return triple.stream().map(Course::getCode).anyMatch(courseService::isUpperDivision);
    }

    // =============== HELPER CLASSES ===============
    @Getter
    @Setter
    public static class ClusterChoice {
        private String sector;       // "AH", "SS", "QS"
        private int theme;          // e.g. 1, 2, 3
        private List<CourseInfo> courses;

        public ClusterChoice(String sector, int theme, List<Course> courses) {
            this.sector = sector;
            this.theme = theme;
            this.courses = courses.stream()
                    .map(course -> new CourseInfo(
                            course.getCode(),
                            course.getTitle(),
                            course.getThemes()
                    ))
                    .collect(Collectors.toList());
        }

        @Getter
        @AllArgsConstructor
        public static class CourseInfo {
            private String code;
            private String name;
            private List<Integer> themes;
        }
    }

    @Getter @Setter
    public static class ClusterSolution {
        private List<ClusterChoice> clusterChoices;

        public ClusterSolution(List<ClusterChoice> clusterChoices) {
            this.clusterChoices = clusterChoices;
        }
    }
}

