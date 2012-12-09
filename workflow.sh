#!/bin/bash
FILE=$1

if [ ! -f $FILE-1-wordnet.ttl ]; then
	java -jar QueryFinder.jar -s nl.vu.queryfinder.services.impl.WordNetExpander		-i $FILE.ttl						-o $FILE-1-wordnet.ttl
fi

if [ ! -f $FILE-2-expand.ttl ]; then
	java -jar QueryFinder.jar -s nl.vu.queryfinder.services.impl.ModelExpander			-i $FILE-1-wordnet.ttl	-o $FILE-2-expand.ttl
fi

if [ ! -f $FILE-3-matcher.ttl ]; then
	java -jar QueryFinder.jar -s nl.vu.queryfinder.services.impl.SPARQLMatcher			-i $FILE-2-expand.ttl		-o $FILE-3-matcher.ttl
fi

if [ ! -f $FILE-4-filter.ttl ]; then
	java -jar QueryFinder.jar -s nl.vu.queryfinder.services.impl.AskFilter					-i $FILE-3-matcher.ttl	-o $FILE-4-filter.ttl
fi

if [ ! -f $FILE-5-solver.ttl ]; then
	java -jar QueryFinder.jar -s nl.vu.queryfinder.services.impl.EvolutionarySolver	-i $FILE-4-filter.ttl		-o $FILE-5-solver.ttl
fi



