package network.nerve.swap;

/**
 * @author Niels
 */
public interface JunitExecuter<T> {
    Object execute(JunitCase<T> junitCase);
}
