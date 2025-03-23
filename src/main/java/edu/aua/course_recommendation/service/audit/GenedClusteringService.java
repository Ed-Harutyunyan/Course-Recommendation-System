package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import edu.aua.course_recommendation.model.NeededCluster;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class GenedClusteringService {

    private static final Set<Integer> AH_THEMES = Set.of(1, 2, 3);
    private static final Set<Integer> SS_THEMES = Set.of(4, 5, 6);
    private static final Set<Integer> QS_THEMES = Set.of(7, 8, 9);

    /**
     * Returns a set of "NeededCluster" objects describing
     * which themes the student is short on (like "need 1 upper for theme 7").
     */
    public Set<NeededCluster> findNeededClusters(List<Course> completedCourses) {
        Set<NeededCluster> needed = new HashSet<>();

        // We'll do a partial analysis for AH, SS, QS

        // 1. Try to form an AH triple (themes 1,2,3)
        boolean canFormAH = false;
        for (int theme : AH_THEMES) {
            if (canFormTriple(completedCourses, theme)) {
                canFormAH = true;
                break;
            }
        }
        if (!canFormAH) {
            // If we can't form a triple in ANY AH theme,
            // let's see which theme is "closest" or which theme we might want to fill
            needed.addAll(analyzeThemeShortage(completedCourses, AH_THEMES));
        }

        // 2. Try to form SS triple
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

        // 3. Try to form QS triple
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

    private Set<NeededCluster> analyzeThemeShortage(List<Course> courses, Set<Integer> sectorThemes) {
        Set<NeededCluster> needed = new HashSet<>();

        for (int theme : sectorThemes) {
            // If I can't form a triple with this theme,
            // let's see how close we are.
            if (canFormTriple(courses, theme)) {
                // We can form a triple in this theme => not missing
                continue;
            }
            // else let's do partial analysis
            needed.add(analyzeSingleTheme(courses, theme));
        }

        return needed;
    }

    private NeededCluster analyzeSingleTheme(List<Course> courses, int theme) {
        // 1. Filter courses that contain 'theme'
        List<Course> filtered = courses.stream()
                .filter(c -> c.getClusters().contains(theme))
                .toList();

        // 2. Count how many lower vs upper
        int lowerCount = 0, upperCount = 0;
        for (Course c : filtered) {
            if (isLowerDivision(c)) lowerCount++;
            else if (isUpperDivision(c)) upperCount++;
        }

        // 3. We want a triple => total 3 courses, at least 1 lower, 1 upper
        // Cases:
        //  - if lowerCount == 0 => definitely missing at least 1 lower
        //  - if upperCount == 0 => missing at least 1 upper
        //  - if lowerCount + upperCount < 3 => missing some total
        // We'll store that info in a new "NeededCluster" object

        // for simplicity:
        NeededCluster needed = new NeededCluster();
        needed.setTheme(theme);

        if (lowerCount < 1) {
            needed.setMissingLower(1 - lowerCount); // at least 1
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



    /**
     * Quickly checks if the GenEd requirement is met.
     * i.e., if there's at least one valid set of 3 clusters (AH, SS, QS),
     * each with a single theme, containing 3 courses (≥1 lower, ≥1 upper).
     */
    public boolean isGenEdRequirementMet(List<Course> completedCourses) {
        // We'll try to find at least 1 solution
        List<ClusterSolution> solutions = findPossibleClusterCombinations(completedCourses, 1);
        return !solutions.isEmpty(); // if we found at least one, it's met
    }

    /**
     * Finds up to 'maxSolutions' possible ways to form the 3 GenEd clusters
     * (AH, SS, QS). Each cluster must have exactly 3 courses sharing a single theme,
     * with at least 1 lower-division and 1 upper-division course.
     * @return [] if no clusters
     */
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
            // All valid 3-course combos for this theme
            List<List<Course>> validTriples = findValidTriples(available, theme);

            for (List<Course> triple : validTriples) {
                // Add this triple as the AH cluster
                ClusterChoice ahChoice = new ClusterChoice("AH", theme, triple);

                // Copy the chosen clusters so far
                List<ClusterChoice> newChosen = new ArrayList<>(chosen);
                newChosen.add(ahChoice);

                // Remove these courses from available
                List<Course> newAvailable = new ArrayList<>(available);
                newAvailable.removeAll(triple);

                // Move on to SS
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

                // Move on to QS
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

                // We now have AH, SS, QS => one complete solution
                solutions.add(new ClusterSolution(newChosen));
                if (solutions.size() >= maxSolutions) return;
            }
        }
    }

    // =============== FIND VALID TRIPLES ===============
    /**
     * Finds all possible 3-course subsets from 'available' that contain 'theme',
     * with ≥1 lower-division and ≥1 upper-division.
     */
    private List<List<Course>> findValidTriples(List<Course> available, int theme) {
        // Filter courses that have this theme
        List<Course> filtered = available.stream()
                .filter(c -> c.getClusters().contains(theme))
                .toList();

        // Generate all 3-combinations
        List<List<Course>> combos = combinationsOf3(filtered);
        // Validate each triple
        List<List<Course>> valid = new ArrayList<>();
        for (List<Course> triple : combos) {
            if (hasAtLeastOneLower(triple) && hasAtLeastOneUpper(triple)) {
                valid.add(triple);
            }
        }
        return valid;
    }

    // Can a valid cluster be formed for courses
    private boolean canFormTriple(List<Course> courses, int theme) {
        List<List<Course>> valid = findValidTriples(courses, theme);
        return !valid.isEmpty();
    }

    /**
     * Generates all 3-element subsets of the given list (simple approach).
     */
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
        return triple.stream().anyMatch(this::isLowerDivision);
    }

    private boolean hasAtLeastOneUpper(List<Course> triple) {
        return triple.stream().anyMatch(this::isUpperDivision);
    }

    /**
     * If the code starts with '1', then its lower-division.
     * Otherwise upper-division.
     */
    private boolean isLowerDivision(Course c) {
        String code = c.getCode().replaceAll("[^0-9]", "");
        if (code.isEmpty()) return false;

        try {
            int number = Integer.parseInt(code);
            return number < 200; // Lower division courses are numbered < 200
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private boolean isUpperDivision(Course c) {
        return !isLowerDivision(c);
    }

    // =============== HELPER CLASSES ===============
    @Getter
    @Setter
    public static class ClusterChoice {
        private String sector;       // "AH", "SS", "QS"
        private int theme;          // e.g. 1, 2, 3
        private List<Course> courses;

        public ClusterChoice(String sector, int theme, List<Course> courses) {
            this.sector = sector;
            this.theme = theme;
            this.courses = courses;
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

