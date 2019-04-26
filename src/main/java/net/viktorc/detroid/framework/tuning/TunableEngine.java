package net.viktorc.detroid.framework.tuning;

import java.util.Map;
import net.viktorc.detroid.framework.uci.UCIEngine;

/**
 * An interface for a tunable chess engine whose parameters can be retrieved.
 *
 * @author Viktor
 */
public interface TunableEngine extends UCIEngine {

  /**
   * Returns the parameters which are used by the engine to control search, evaluation, and other aspects of game play.
   *
   * @return The engine parameters.
   */
  EngineParameters getParameters();

  /**
   * Notifies the engine that the parameters have changed and that if it uses cached values, it should reload them. If reloading a parameter
   * would affect a UCI setting whose current value is not the default value, it should not be reloaded. (E.g. if the hash size is defined
   * in the parameter configuration XML file, but the UCI option "Hash" has been set to a value other than its default value, the hash size
   * used should remain unchanged.)
   */
  void notifyParametersChanged();

  /**
   * Sets whether the engine should support deterministic static evaluation without the use of hash tables  or any other mechanisms that
   * introduce non-determinism.
   *
   * @param on Whether the engine should support deterministic evaluation mode.
   */
  void setDeterministicEvaluationMode(boolean on);

  /**
   * Statically evaluates the current position and records the gradient of the evaluation function w.r.t. the parameters in the provided
   * map.
   *
   * @param gradientCache A map for recording the partial derivatives of the objective evaluation function (where a positive output means
   * white is in the lead and negative output means black is in the lead) with respect to the static evaluation parameters. The key
   * should be the name of the parameter and the value should be the derivative. If {@link #isGradientDefined()} returns false, it is null.
   * @return The static evaluation score of the position.
   */
  short eval(Map<String, Double> gradientCache);

  /**
   * Specifies whether the gradient of the evaluation function is mathematically defined. If it is not, numerical differentiation is
   * used to approximate the gradient when needed. If it is, the partial derivatives are resolved based on the entries of the
   * {@code gradientCache} parameter of the {@link #eval(Map)} method.
   *
   * @return Whether the symbolic gradient of the evaluation function is defined.
   */
  default boolean isGradientDefined() {
    return true;
  }

}
