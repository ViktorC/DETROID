package net.viktorc.detroid.framework;

import net.viktorc.detroid.framework.engine.Detroid;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * An interface for factory classes providing new engine instances.
 *
 * @author Viktor
 */
public interface EngineFactory {

  /**
   * Returns a new {@link net.viktorc.detroid.framework.uci.UCIEngine} instance.
   *
   * @return A new UCI compatible chess engine instance.
   */
  UCIEngine newEngineInstance();

  /**
   * Returns a new {@link net.viktorc.detroid.framework.tuning.TunableEngine} instance.
   *
   * @return A new tunable chess engine instance.
   */
  default TunableEngine newTunableEngineInstance() {
    return (TunableEngine) newEngineInstance();
  }

  /**
   * Returns a new {@link net.viktorc.detroid.framework.validation.ControllerEngine} instance.
   *
   * @return A new controller engine instance.
   */
  default ControllerEngine newControllerEngineInstance() {
    return new Detroid();
  }

}