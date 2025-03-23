package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.entity.Course;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Service
public class GenedClusteringService {

    private static final Set<Integer> AH_THEMES = Set.of(1, 2, 3);
    private static final Set<Integer> SS_THEMES = Set.of(4, 5, 6);
    private static final Set<Integer> QS_THEMES = Set.of(7, 8, 9);

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

