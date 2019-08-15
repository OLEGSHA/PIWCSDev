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

	void crash(Exception exception, String message);
	
}
