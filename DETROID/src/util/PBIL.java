package util;

import java.util.Arrays;
import java.util.Observable;
import java.util.Random;

/**
 * An abstract Population-based Incremental Learning algorithm implementation for optimizing parameters represented
 * by strings of binary digits. The fitness of the individuals in each population is measured by the abstract method
 * {@link #fitnessFunction(String) fitnessFunction} which must be implemented. Instances of this class and its
 * subclasses are observable and the observers are notified every time the fittest genome changes during the optimization
 * process.
 * 
 * @author Viktor
 * 
 */
public abstract class PBIL extends Observable {
	
	/**
	 * The default population size.
	 */
	public final static int POPULATION_SIZE = 100;
	/**
	 * The default number of iterations performed during the optimization.
	 */
	public final static int GENERATIONS = 1000;
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
	
	private final int genomeLength;
	private final int populationSize;
	private final int generations;
	private final double mutationProbability;
	private final double mutationShift;
	private final double learningRate;
	private final double negLearningRateAddition;
	
	private double[] probabilityVector;
	
	private Individual allTimeFittestIndividual;
	private Individual currentFittestIndividual;
	private Individual currentLeastFitIndividual;
	private int currentIndividualIndex;
	private int currentGeneration;
	private double averageCurrentGenerationFitness;
	
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
	 * @param genomeLength The number of genes in the genomes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param mutationProbability The probability of mutation for each gene.
	 * @param mutationShift The effect of mutation on the probability vector.
	 * @param learningRate The effect the fittest specimen's genome has on the probability vector.
	 * @param negLearningRateAddition The additional effect the fittest specimen's genes have on
	 * the probability vector at the points where they differ from those of the least fit specimen
	 * in the population.
	 */
	protected PBIL(double[] initialProbabilityVector, int genomeLength, int populationSize, int generations,
			double mutationProbability, double mutationShift, double learningRate, double negLearningRateAddition) {
		if (genomeLength <= 0)
			throw new IllegalArgumentException("The value of genomeLength has to be greater than 0.");
		if (initialProbabilityVector == null) {
			// Start with an unbiased probability vector.
			probabilityVector = new double[genomeLength];
			Arrays.fill(probabilityVector, 0.5d);
		} else {
			probabilityVector = Arrays.copyOf(initialProbabilityVector, genomeLength);
			if (initialProbabilityVector.length < genomeLength)
				Arrays.fill(probabilityVector, initialProbabilityVector.length, genomeLength, 0.5d);
		}
		allTimeFittestIndividual = new Individual(null, -Double.MAX_VALUE);
		currentFittestIndividual = new Individual();
		currentLeastFitIndividual = new Individual();
		this.genomeLength = genomeLength;
		this.populationSize = populationSize;
		this.generations = generations;
		this.mutationProbability = mutationProbability;
		this.mutationShift = mutationShift;
		this.learningRate = learningRate;
		this.negLearningRateAddition = negLearningRateAddition;
	}
	/**
	 * Constructs an instance for the specified genome length, population size, and number of generations with the default
	 * optimization parameters.
	 * 
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than genomeLength, it will be extended with
	 * elements of the value 0.5d; if the length of the array is greater than genomeLength, only
	 * the first x elements will be considered, where x = genomeLength. If it is null, an array
	 * with a length equal to genomeLength, only containing elements that have the value 0.5d
	 * will be used.
	 * @param genomeLength The number of genes in the genomes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 */
	protected PBIL(double[] initialProbabilityVector, int genomeLength, int populationSize, int generations) {
		this(initialProbabilityVector, genomeLength, populationSize, generations, MUTATION_PROBABILITY,
				MUTATION_SHIFT, LEARNING_RATE, NEGATIVE_LEARNING_RATE);
	}
	/**
	 * Constructs an instance for the specified genome length, population size, and number of generations with the default
	 * optimization parameters.
	 * 
	 * @param genomeLength The number of genes in the genomes, i.e. the number of binary digits
	 * needed to represent the parameters to be optimized. It has to be greater than 0 or an
	 * {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 */
	protected PBIL(int genomeLength, int populationSize, int generations) {
		this(null, genomeLength, populationSize, generations);
	}
	/**
	 * Returns the population size.
	 * 
	 * @return
	 */
	public int getPopulationSize() {
		return populationSize;
	}
	/**
	 * Returns the total number of generations
	 * 
	 * @return
	 */
	public int getGenerations() {
		return generations;
	}
	/**
	 * Returns a copy of the current probability vector. It returns null if {@link #optimize() optimze} has not been
	 * called on the instance yet.
	 * 
	 * @return
	 */
	public double[] getProbabilityVector() {
		return Arrays.copyOf(probabilityVector, probabilityVector.length);
	}
	/**
	 * Returns the individual represented by its genome and fitness level that achieved the highest fitness score so
	 * far during the optimization process. It returns null if {@link #optimize() optimze} has not been called on the
	 * instance yet.
	 * 
	 * @return
	 */
	public Individual getAllTimeFittestIndividual() {
		return allTimeFittestIndividual;
	}
	/**
	 * Returns the fittest individual represented by its genome and fitness level that achieved the highest fitness
	 * score so far in the current generation. It returns null if {@link #optimize() optimze} has not been called on
	 * the instance yet.
	 * 
	 * @return
	 */
	public Individual getCurrentFittestIndividual() {
		return currentFittestIndividual;
	}
	/**
	 * Returns the least fit individual represented by its genome and fitness level that achieved the lowest fitness
	 * score so far in the current generation. It returns null if {@link #optimize() optimze} has not been called on
	 * the instance yet.
	 * 
	 * @return
	 */
	public Individual getCurrentLeastFitIndividual() {
		return currentLeastFitIndividual;
	}
	/**
	 * Returns the index (starting from 1) of individual whose fitness level is currently being assessed.
	 * 
	 * @return
	 */
	public int getCurrentIndividualIndex() {
		return currentIndividualIndex;
	}
	/**
	 * Returns the index (starting from 1) of the current generation in the evolution.
	 * 
	 * @return
	 */
	public int getCurrentGenerationIndex() {
		return currentGeneration;
	}
	/**
	 * Returns the average fitness level of the current generation.
	 * 
	 * @return
	 */
	public double getAverageCurrentGenerationFitness() {
		return averageCurrentGenerationFitness;
	}
	/**
	 * Sets the status of the object to changed and notifies all observers.
	 */
	private void informObservers() {
		setChanged();
		notifyObservers();
	}
	/**
	 * An implementation of the Population-based Incremental Learning algorithm which optimizes
	 * a string of binary digits representing parameters to a system according to the instance's
	 * constructor parameters. It this method is invoked on an instance for the second time, it
	 * will continue the optimization where the first method call left off. For starting anew, a
	 * new instance needs to be created.
	 * 
	 * @return The fittest individual.
	 */
	public synchronized Individual optimize() {
		final int prevGen = currentGeneration;
		Random rand = new Random(System.nanoTime());
		// Evolution.
		for (int i = prevGen; i < generations + prevGen; i++) {
			double cummulativeFitness = 0;
			currentGeneration = i + 1;
			averageCurrentGenerationFitness = 0;
			currentFittestIndividual = new Individual(null, -Double.MAX_VALUE);
			currentLeastFitIndividual = new Individual(null, Double.MAX_VALUE);
			// Generate the new genomes and set up the new population.
			String[] genomes = new String[populationSize];
			for (int j = 0; j < populationSize; j++) {
				currentIndividualIndex = j +1;
				String genome = "";
				for (int k = 0; k < genomeLength; k++)
					genome += rand.nextDouble() < probabilityVector[k] ? "1" : "0";
				genomes[j] = genome;
				// Measure the individual's fitness
				double fitness = fitnessFunction(genome);
				cummulativeFitness += fitness;
				averageCurrentGenerationFitness = cummulativeFitness/(j + 1);
				// Find the genomes that produced the fittest and the least fit individuals.
				if (fitness > currentFittestIndividual.fitness) {
					currentFittestIndividual = new Individual(genome, fitness);
					if (fitness > allTimeFittestIndividual.fitness) {
						allTimeFittestIndividual = currentFittestIndividual;
					}
				}
				if (fitness < currentLeastFitIndividual.fitness)
					currentLeastFitIndividual = new Individual(genome, fitness);
				informObservers();
			}
			/*
			 * Update the probability vector according to the fitness of the fittest and the least fit
			 * individuals in the population sample and mutate it.
			 */
			for (int j = 0; j < genomeLength; j++) {
				/**
				 * At the points where the fittest genome's gene differs from the least fit genome's gene,
				 * magnify the effect on the probability vector by the value of diffLearningRateAddition.
				 */
				double appliedLearningRate = (currentLeastFitIndividual.genome.charAt(j) == 
						currentFittestIndividual.genome.charAt(j) ? learningRate : 
						learningRate + negLearningRateAddition);
				double newProbabilityVectorVal = probabilityVector[j]*(1d - appliedLearningRate) +
						(currentFittestIndividual.genome.charAt(j) == '1' ? appliedLearningRate : 0d);
				// Mutate the probability vector.
				if (rand.nextDouble() < mutationProbability)
					newProbabilityVectorVal = newProbabilityVectorVal*(1d - mutationShift) +
							(rand.nextBoolean() ? mutationShift : 0d);
				probabilityVector[j] = newProbabilityVectorVal;
			}
		}
		return allTimeFittestIndividual;
	}
	/**
	 * Measures the fitness of the genome. Higher values mean higher fitness levels. It is a crucial step,
	 * as the genes will be optimized to increase the value returned by this function.
	 * 
	 * @param genome The genome represented by a string of binary digits of the specified length.
	 * @return
	 */
	protected abstract double fitnessFunction(String genome);
	
	/**
	 * An immutable class representing an individual from a population by its genome and fitness level.
	 * 
	 * @author Viktor
	 *
	 */
	public final static class Individual {
		
		private final String genome;
		private final double fitness;
		
		/**
		 * Constructs a default instance.
		 */
		private Individual() {
			genome = null;
			fitness = 0;
		}
		/**
		 * Constructs an individual according to the specified parameters.
		 * 
		 * @param genome
		 * @param fitness
		 */
		private Individual(String genome, double fitness) {
			this.genome = genome;
			this.fitness = fitness;
		}
		/**
		 * Returns the genome of the individual represented by a string of binary digits.
		 * 
		 * @return
		 */
		public String getGenome() {
			return genome;
		}
		/**
		 * Returns the fitness level of the individual as measured by the {@link #PBIL.fitnessFunction(String) fitnessFunction}.
		 * 
		 * @return
		 */
		public double getFitness() {
			return fitness;
		}
	}
}
