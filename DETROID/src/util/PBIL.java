package util;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * An abstract Population-based Incremental Learning algorithm implementation for optimizing parameters represented
 * by strings of binary digits. The fitness of the individuals in each population is measured by the abstract method
 * {@link #fitnessFunction(String) fitnessFunction} which must be implemented.
 * 
 * @author Viktor
 * 
 */
public abstract class PBIL {
	
	/**
	 * The default probability of mutation applied to the probability vector.
	 */
	public final static double MUTATION_PROBABILITY = 0.02d;
	/**
	 * The default amount of mutation applied to the probability vector.
	 */
	public final static double MUTATION_SHIFT = 0.05d;
	/**
	 * The default learning rate.
	 */
	public final static double LEARNING_RATE = 0.1d;
	/**
	 * The default additional learning rate from 'negative' experience.
	 */
	public final static double NEGATIVE_LEARNING_RATE = 0.025d;
	/**
	 * The value used for the generation field in case no limit has been specified.
	 */
	protected final static int NO_GENERATION_CAP = -1;
	
	protected final int genotypeLength;
	protected final int populationSize;
	protected final int generations;
	protected final double mutationProbability;
	protected final double mutationShift;
	protected final double learningRate;
	protected final double negLearningRateAddition;
	protected double[] probabilityVector;
	
	protected final Logger logger;
	
	/**
	 * Constructs an instance with the specified optimization parameters.
	 * 
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than genomeLength, it will be extended with
	 * elements of the value 0.5d; if the length of the array is greater than genomeLength, only
	 * the first x elements will be considered, where x = genomeLength. If it is null, an array
	 * with a length equal to genomeLength, only containing elements that have the value 0.5d
	 * will be used.
	 * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param mutationProbability The probability of mutation for each gene.
	 * @param mutationShift The effect of mutation on the probability vector.
	 * @param learningRate The effect the fittest specimen's genotype has on the probability vector.
	 * @param negLearningRateAddition The additional effect the fittest specimen's genes have on
	 * the probability vector at the points where they differ from those of the least fit specimen
	 * in the population.
	 * @param logger The logger used to log the current status of the optimization process. If it is 
	 * null, no logging is performed.
	 */
	protected PBIL(double[] initialProbabilityVector, int genotypeLength, int populationSize, int generations,
			double mutationProbability, double mutationShift, double learningRate, double negLearningRateAddition,
			Logger logger) {
		if (genotypeLength <= 0)
			throw new IllegalArgumentException("The value of genomeLength has to be greater than 0.");
		if (initialProbabilityVector == null) {
			// Start with an unbiased probability vector.
			probabilityVector = new double[genotypeLength];
			Arrays.fill(probabilityVector, 0.5d);
		} else {
			probabilityVector = Arrays.copyOf(initialProbabilityVector, genotypeLength);
			if (initialProbabilityVector.length < genotypeLength)
				Arrays.fill(probabilityVector, initialProbabilityVector.length, genotypeLength, 0.5d);
		}
		this.genotypeLength = genotypeLength;
		this.populationSize = populationSize;
		this.generations = generations;
		this.mutationProbability = mutationProbability;
		this.mutationShift = mutationShift;
		this.learningRate = learningRate;
		this.negLearningRateAddition = negLearningRateAddition;
		this.logger = logger;
	}
	/**
	 * Constructs an instance for the specified initial probability vector, genotype length, population size, and number of
	 * generations with the default optimization parameters.
	 * 
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than genomeLength, it will be extended with
	 * elements of the value 0.5d; if the length of the array is greater than genomeLength, only
	 * the first x elements will be considered, where x = genomeLength. If it is null, an array
	 * with a length equal to genomeLength, only containing elements that have the value 0.5d
	 * will be used.
	 * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param logger The logger used to log the current status of the optimization process. If it is 
	 * null, no logging is performed.
	 */
	protected PBIL(double[] initialProbabilityVector, int genotypeLength, int populationSize, int generations, Logger logger) {
		this(initialProbabilityVector, genotypeLength, populationSize, generations, MUTATION_PROBABILITY,
				MUTATION_SHIFT, LEARNING_RATE, NEGATIVE_LEARNING_RATE, logger);
	}
	/**
	 * Constructs an instance for the specified initial probability vector, genotype length, population size, and number
	 * of generations with the default optimization parameters. The set will be considered optimized, and thus the
	 * process will terminate, when all the elements of the probability vector have converged to 0 or 1 within a margin
	 * dependent on the population size [1/populationSize]. If the initial probability vector is already converged, no
	 * further optimization will be performed.
	 * 
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than genomeLength, it will be extended with
	 * elements of the value 0.5d; if the length of the array is greater than genomeLength, only
	 * the first x elements will be considered, where x = genomeLength. If it is null, an array
	 * with a length equal to genomeLength, only containing elements that have the value 0.5d
	 * will be used.
	 * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param logger The logger used to log the current status of the optimization process. If it is 
	 * null, no logging is performed.
	 */
	protected PBIL(double[] initialProbabilityVector, int genotypeLength, int populationSize, Logger logger) {
		this(initialProbabilityVector, genotypeLength, populationSize, NO_GENERATION_CAP, MUTATION_PROBABILITY,
				MUTATION_SHIFT, LEARNING_RATE, NEGATIVE_LEARNING_RATE, logger);
	}
	/**
	 * Constructs an instance for the specified genotype length, population size, and number of generations with the default
	 * optimization parameters.
	 * 
	 * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param logger The logger used to log the current status of the optimization process. If it is 
	 * null, no logging is performed.
	 */
	protected PBIL(int genotypeLength, int populationSize, int generations, Logger logger) {
		this(null, genotypeLength, populationSize, generations, logger);
	}
	/**
	 * Constructs an instance for the specified genotype length and population size with the default optimization parameters.
	 * The set will be considered optimized, and thus the process will terminate, when all the elements of the probability
	 * vector have converged to 0 or 1 within a margin dependent on the population size [1/populationSize].
	 * 
	 * @param genotypeLength The number of genes in the genotypes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param logger The logger used to log the current status of the optimization process. If it is 
	 * null, no logging is performed.
	 */
	protected PBIL(int genotypeLength, int populationSize, Logger logger) {
		this(null, genotypeLength, populationSize, NO_GENERATION_CAP, logger);
	}
	/**
	 * Returns the Shannon entropy of the current generation of genotypes.
	 * 
	 * @return
	 */
	public final double getEntropy() {
		double entropy = 0;
		for (double d : probabilityVector)
			entropy += -d*Math.log(d)/Math.log(2);
		return entropy;
	}
	/**
	 * Returns whether the number of processed generations has reached the predefined limit or if
	 * none was defined, whether all the elements of the probability vector have converged to
	 * either 0 or 1.
	 * 
	 * @param currentGeneration
	 * @return Whether the loop can be broken or not.
	 */
	private final boolean isOptimized(int currentGeneration) {
		if (generations == NO_GENERATION_CAP) {
			for (double e : probabilityVector) {
				if (e > 1d/populationSize && e < 1d - 1d/populationSize)
					return false;
			}
			return true;
		} else
			return currentGeneration >= generations;
	}
	/**
	 * An implementation of the Population-based Incremental Learning algorithm which optimizes
	 * a string of binary digits representing parameters to a system according to the instance's
	 * constructor parameters.
	 * 
	 * @return The fittest individual.
	 */
	public final synchronized String optimize() {
		Random rand = new Random(System.nanoTime());
		String[] genotypes = new String[populationSize];
		double highestFitness = -Double.MAX_VALUE;
		String fittestGenotype = null;
		int curGen = 0;
		// Evolution.
		while (true) {
			if (logger != null)
				logger.info("Generation: " + curGen + "; Entropy: " + getEntropy() + System.lineSeparator() + 
						"Probability vector: " + probabilityVector + System.lineSeparator());
			// Generate the new population by generating the genotypes using the probability vector.
			for (int i = 0; i < populationSize; i++) {
				String genotype = "";
				for (int k = 0; k < genotypeLength; k++)
					genotype += (rand.nextDouble() < probabilityVector[k] ? "1" : "0");
				genotypes[i] = genotype;
			}
			String curFittestGenotype = null;
			String curLeastFitGenotype = null;
			double curHighestFitness = -Double.MAX_VALUE;
			double curLowestFitness = Double.MIN_VALUE;
			double averageCurrentGenerationFitness = 0;
			// Measure the fitness of each individual in the population.
			for (int i = 0; i < populationSize; i++) {
				String genome = genotypes[i];
				double fitness = fitnessFunction(genome);
				averageCurrentGenerationFitness = (i*averageCurrentGenerationFitness + fitness)/(i + 1);
				// Track the genotypes responsible for the fittest and least fit individuals.
				if (fitness > curHighestFitness) {
					curHighestFitness = fitness;
					curFittestGenotype = genome;
					if (fitness > highestFitness) {
						fittestGenotype = genome;
					}
				}
				if (fitness < curLowestFitness) {
					curLowestFitness = fitness;
					curLeastFitGenotype = genome;
				}
			}
			if (logger != null)
				logger.info("Average fitness: " + averageCurrentGenerationFitness + System.lineSeparator() + 
						"All time fittest genotype: " + fittestGenotype + System.lineSeparator());
			/*
			 * Update the probability vector according to the fitness of the fittest and the least fit
			 * individuals in the population sample and mutate it.
			 */
			for (int j = 0; j < genotypeLength; j++) {
				/**
				 * At the points where the fittest genotype's gene differs from the least fit genome's gene,
				 * magnify the effect on the probability vector by the value of diffLearningRateAddition.
				 */
				double appliedLearningRate = (curLeastFitGenotype.charAt(j) == curFittestGenotype.charAt(j) ? learningRate : 
						learningRate + negLearningRateAddition);
				double newProbabilityVectorVal = probabilityVector[j]*(1d - appliedLearningRate) +
						(curFittestGenotype.charAt(j) == '1' ? appliedLearningRate : 0d);
				// Mutate the probability vector.
				if (rand.nextDouble() < mutationProbability)
					newProbabilityVectorVal = newProbabilityVectorVal*(1d - mutationShift) +
							(rand.nextBoolean() ? mutationShift : 0d);
				probabilityVector[j] = newProbabilityVectorVal;
			}
			curGen++;
			// Exit if the evolution has reached the desired stage.
			if (isOptimized(curGen))
				break;
		}
		return fittestGenotype;
	}
	/**
	 * Measures the fitness of the genotype. Higher values mean higher fitness levels. It is a crucial step,
	 * as the genes will be optimized to increase the value returned by this function.
	 * 
	 * @param genotype The genotype represented by a string of binary digits of the specified length.
	 * @return The fitness level of the genotype.
	 */
	protected abstract double fitnessFunction(String genotype);
	
}
