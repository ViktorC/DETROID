package net.viktorc.detroid.framework.util;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * An abstract Population-based Incremental Learning algorithm implementation for optimizing parameters represented by strings of binary
 * digits. The fitness of the individuals in each population is measured by the abstract method {@link #fitnessFunction(String)
 * fitnessFunction} which must be implemented.
 *
 * PBIL: <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.61.8554">http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.61.8554</a>
 *
 * @author Viktor
 */
public abstract class PBIL {

  /**
   * The default number of genotypes generated per generation.
   */
  protected static final int POPULATION_SIZE = 100;
  /**
   * The default probability of mutation applied to the probability vector.
   */
  protected static final double MUTATION_PROBABILITY = 0.02d;
  /**
   * The default amount of mutation applied to the probability vector.
   */
  protected static final double MUTATION_SHIFT = 0.05d;
  /**
   * The default learning rate.
   */
  protected static final double LEARNING_RATE = 0.1d;
  /**
   * The default additional learning rate from 'negative' experience.
   */
  protected static final double NEGATIVE_LEARNING_RATE = 0.025d;
  /**
   * The maximum difference from 0 or 1 each element of the probability vector can have for the set to be still possibly considered
   * optimized.
   */
  protected static final double MAX_DIVERGENCE = 0.1d;

  private final int genotypeLength;
  private final int populationSize;
  private final double mutationProbability;
  private final double mutationShift;
  private final double learningRate;
  private final double negLearningRateAddition;
  private final Integer generations;

  private final double[] probabilityVector;
  private int currentGeneration;
  private double currentHighestFitness;

  private final Logger logger;

  /**
   * Constructs an instance with the specified optimization parameters.
   *
   * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits needed to represent the parameters to be
   * optimized. It has to be greater than 0 or an {@link java.lang.IllegalArgumentException} is thrown.
   * @param populationSize The number of genotypes to produce per generation. If it is null, a default value of 100 will be used.
   * @param mutationProbability The probability of mutation for each gene. If it is null, a default value of 0.02 will be used.
   * @param mutationShift The effect of mutation on the probability vector. If it is null, a default value of 0.05 will be used.
   * @param learningRate The effect the fittest specimen's genotype has on the probability vector. If it is null a default value of 0.1 will
   * be used.
   * @param negLearningRateAddition The additional effect the fittest specimen's genes have on the probability vector at the points where
   * they differ from those of the least fit specimen in the population.  If it is null a default value of 0.025 will be used.
   * @param generations The number of iterations. If it is null, the set will be considered optimized, and thus the process will terminate,
   * when all the elements of the probability vector have converged to 0 or 1 within a margin dependent on the population size
   * [1/populationSize]. If an initial probability vector is provided and it already converged, no further optimization will be performed.
   * @param initialProbabilityVector The starting probability vector for the optimization. It allows the algorithm to pick up where a
   * previous, terminated optimization process left off. If the array's length is smaller than genomeLength, it will be extended with
   * elements of the value 0.5d; if the length of the array is greater than genomeLength, only the first x elements will be considered,
   * where x = genomeLength. If it is null, an array with a length equal to genomeLength, only containing elements that have the value 0.5d
   * will be used.
   * @param logger The logger used to log the current status of the optimization process. If it is null, no logging is performed.
   * @throws IllegalArgumentException If the length of the genotype length is not greater than 0.
   */
  protected PBIL(int genotypeLength, Integer populationSize, Double mutationProbability, Double mutationShift,
      Double learningRate, Double negLearningRateAddition, Integer generations, double[] initialProbabilityVector,
      Logger logger) throws IllegalArgumentException {
    if (genotypeLength <= 0) {
      throw new IllegalArgumentException("The genotype length has to be greater than 0.");
    }
    if (initialProbabilityVector == null) {
      // Start with an unbiased probability vector.
      probabilityVector = new double[genotypeLength];
      Arrays.fill(probabilityVector, 0.5d);
    } else {
      probabilityVector = Arrays.copyOf(initialProbabilityVector, genotypeLength);
      if (initialProbabilityVector.length < genotypeLength) {
        Arrays.fill(probabilityVector, initialProbabilityVector.length, genotypeLength, 0.5d);
      }
    }
    this.genotypeLength = genotypeLength;
    this.populationSize = (populationSize == null ? POPULATION_SIZE : populationSize);
    this.mutationProbability = (mutationProbability == null ? MUTATION_PROBABILITY : mutationProbability);
    this.mutationShift = (mutationShift == null ? MUTATION_SHIFT : mutationShift);
    this.learningRate = (learningRate == null ? LEARNING_RATE : learningRate);
    this.negLearningRateAddition = (negLearningRateAddition == null ? NEGATIVE_LEARNING_RATE :
        negLearningRateAddition);
    this.generations = generations;
    this.logger = logger;
  }

  /**
   * Returns the index of the current generation in the evolution.
   *
   * @return The index of the current generation in the evolution.
   */
  protected int getCurrentGeneration() {
    return currentGeneration;
  }

  /**
   * Returns the highest fitness score of the current generation so far.
   *
   * @return The highest fitness score of the current generation.
   */
  protected double getCurrentHighestFitness() {
    return currentHighestFitness;
  }

  /**
   * Returns the probability vector representing the current set.
   *
   * @return A copy of the probability vector.
   */
  public double[] getProbabilityVector() {
    return Arrays.copyOf(probabilityVector, probabilityVector.length);
  }

  /**
   * Returns the Shannon entropy of the current generation of genotypes.
   *
   * @return The Shannon entropy of the current generation of genotypes
   */
  public final double getEntropy() {
    double entropy = 0;
    for (double d : probabilityVector) {
      entropy += -d * Math.log(d) / Math.log(2);
    }
    return entropy;
  }

  /**
   * Returns whether the number of processed generations has reached the predefined limit or if none was defined, whether all the elements
   * of the probability vector have converged to either 0 or 1.
   *
   * @param currentGeneration The current generation count.
   * @return Whether the loop can be broken or not.
   */
  private boolean isOptimized(int currentGeneration) {
    if (generations == null) {
      for (double e : probabilityVector) {
        double limit = Math.min(1d / populationSize, MAX_DIVERGENCE);
        if (e > limit && e < 1d - limit) {
          return false;
        }
      }
      return true;
    } else {
      return currentGeneration >= generations;
    }
  }

  /**
   * An implementation of the Population-based Incremental Learning algorithm which optimizes a string of binary digits representing
   * parameters to a system according to the instance's constructor parameters.
   *
   * @return The probability vector.
   */
  public final synchronized double[] optimize() {
    Random rand = new Random(System.nanoTime());
    String[] genotypes = new String[populationSize];
    currentGeneration = 0;
    // Evolution.
    do {
      // Generate the new population by generating the genotypes using the probability vector.
      for (int i = 0; i < populationSize; i++) {
        String genotype = "";
        for (int k = 0; k < genotypeLength; k++) {
          genotype += (rand.nextDouble() < probabilityVector[k] ? "1" : "0");
        }
        genotypes[i] = genotype;
      }
      currentHighestFitness = -Double.MAX_VALUE;
      double curLowestFitness = Double.MAX_VALUE;
      String curFittestGenotype = null;
      String curLeastFitGenotype = null;
      // Measure the fitness of each individual in the population.
      for (int i = 0; i < populationSize; i++) {
        String genome = genotypes[i];
        double fitness = fitnessFunction(genome);
        // Track the genotypes responsible for the fittest and least fit individuals.
        if (fitness > currentHighestFitness) {
          currentHighestFitness = fitness;
          curFittestGenotype = genome;
        }
        if (fitness < curLowestFitness) {
          curLowestFitness = fitness;
          curLeastFitGenotype = genome;
        }
      }
      /* Update the probability vector according to the fitness of the fittest and the least fit
       * individuals in the population sample and mutate it. */
      for (int j = 0; j < genotypeLength; j++) {
        /* At the points where the fittest genotype's gene differs from the least fit genome's gene,
         * magnify the effect on the probability vector by the value of diffLearningRateAddition. */
        double appliedLearningRate = (curLeastFitGenotype.charAt(j) == curFittestGenotype.charAt(j) ?
            learningRate : learningRate + negLearningRateAddition);
        double newProbabilityVectorVal = probabilityVector[j] * (1d - appliedLearningRate) +
            (curFittestGenotype.charAt(j) == '1' ? appliedLearningRate : 0d);
        // Mutate the probability vector.
        if (rand.nextDouble() < mutationProbability) {
          newProbabilityVectorVal = newProbabilityVectorVal * (1d - mutationShift) +
              (rand.nextBoolean() ? mutationShift : 0d);
        }
        probabilityVector[j] = newProbabilityVectorVal;
      }
      if (logger != null) {
        logger.info("Generation: " + currentGeneration + "; Entropy: " + getEntropy() +
            System.lineSeparator() + "Probability vector: " + Arrays.toString(probabilityVector));
      }
      currentGeneration++;
    } while (isOptimized(currentGeneration));
    return getProbabilityVector();
  }

  /**
   * Measures the fitness of the genotype. Higher values mean higher fitness levels. It is a crucial step, as the genes will be optimized to
   * increase the value returned by this function.
   *
   * @param genotype The genotype represented by a string of binary digits of the specified length.
   * @return The fitness level of the genotype.
   */
  protected abstract double fitnessFunction(String genotype);

}
