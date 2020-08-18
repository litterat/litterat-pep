package io.litterat.pep;

public interface PepContextResolver {

	PepDataClass resolve(Class<?> clss) throws PepException;
}
