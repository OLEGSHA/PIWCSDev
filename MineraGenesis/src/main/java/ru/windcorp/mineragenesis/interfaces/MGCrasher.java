/**
 * 
 */
package ru.windcorp.mineragenesis.interfaces;

/**
 * @author javaponyportable
 *
 */
@FunctionalInterface
public interface MGCrasher {

	void crash(Throwable exception, String message);
	
}
