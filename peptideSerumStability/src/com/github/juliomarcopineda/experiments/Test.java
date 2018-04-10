package com.github.juliomarcopineda.experiments;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.juliomarcopineda.FragmentAnalyzer;
import com.github.juliomarcopineda.peptide.Peptide;
import com.github.juliomarcopineda.peptide.PeptideType;

public class Test {
	public static void main(String[] args) {
		// Prevent equal connections
		// Prevent connections in decreasing order
		
		String peptideSequence = "CGYEQDPWGVRYWYGCKKKKB";
		
		for (int conn1 = 0; conn1 < peptideSequence.length() - 1; conn1++) {
			for (int conn2 = conn1 + 1; conn2 < peptideSequence.length(); conn2++) {
				if (conn1 == conn2) {
					continue;
				}
				
				List<Integer> connections = Arrays.asList(conn1, conn2);
				
				for (PeptideType type : PeptideType.values()) {
					
					try {
						Map<Integer, List<Integer>> graph = createGraphStructure(peptideSequence, connections, type);
						
						Peptide peptide = new Peptide();
						peptide.setSequence(peptideSequence);
						peptide.setConnections(connections);
						peptide.setType(type);
						peptide.setGraph(graph);
						
						FragmentAnalyzer analyzer = new FragmentAnalyzer(peptide);
						analyzer.findAllFragments()
							.measureAllFragmentWeights();
					}
					catch (Exception e) {
						System.out.println("ERROR!");
						System.out.println(type);
						System.out.println("Connection 1: " + conn1);
						System.out.println("Connection 2: " + conn2);
						
						e.printStackTrace();
						System.exit(-1);
						
					}
					
				}
			}
		}
		
		//		Map<String, Double> fragments = analyzer.getFragmentWeights();
		//				fragments.entrySet()
		//					.stream()
		//					.filter(e -> e.getKey()
		//						.contains("#"))
		//					.forEach(System.out::println);
		
		//		double threshold = 5.0;
		//		double massSpecData = 655.74;
		//		
		//		Map<String, Double> suggestedFragments = analyzer.suggestFragments(massSpecData, threshold);
		//		suggestedFragments.entrySet()
		//			.stream()
		//			.forEach(System.out::println);
	}
	
	private static Map<Integer, List<Integer>> createGraphStructure(String peptideSequence, List<Integer> connections, PeptideType type) {
		// Create the graph structure of the peptide base sequence
		Map<Integer, List<Integer>> graph = IntStream.range(0, peptideSequence.length() - 1)
			.mapToObj(source -> {
				int target = source + 1;
				
				List<Integer> targets = new ArrayList<>();
				targets.add(target);
				
				return new AbstractMap.SimpleEntry<Integer, List<Integer>>(source, targets);
			})
			.sorted(Map.Entry.comparingByKey())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (collision1, collision2) -> collision1, LinkedHashMap::new));
		
		// Add any cyclic connections if connections is not empty
		if (!connections.isEmpty()) {
			switch (type) {
				case AMIDE:
					for (int i = 0; i < connections.size(); i++) {
						if (i % 2 == 0) {
							graph.get(connections.get(i))
								.add(connections.get(i + 1));
						}
						else {
							if (!graph.containsKey(connections.get(i))) { // connection is at the end of peptide
								List<Integer> target = new ArrayList<>();
								target.add(connections.get(i - 1));
								
								graph.put(connections.get(i), target);
							}
							else { // second connection is not at the end of peptide
								graph.get(connections.get(i))
									.add(connections.get(i - 1));
							}
						}
					}
					
					break;
				
				case DFBP:
					int dfbpIndex = peptideSequence.length();
					
					for (int connection : connections) {
						// Add connections from DFBP
						if (!graph.containsKey(dfbpIndex)) {
							List<Integer> targets = new ArrayList<>();
							targets.add(connection);
							
							graph.put(dfbpIndex, targets);
						}
						else {
							graph.get(dfbpIndex)
								.add(connection);
						}
						
						// Add connections to DFBP
						if (!graph.containsKey(connection)) { // second connection is at the end of graph
							List<Integer> target = new ArrayList<>();
							target.add(dfbpIndex);
							
							graph.put(connection, target);
						}
						else {
							graph.get(connection)
								.add(dfbpIndex);
						}
						
					}
					
					break;
				case DISULFIDE:
					// Create disulfide bridge
					int s1Index = peptideSequence.length();
					int s2Index = s1Index + 1;
					
					List<Integer> s1ToS2 = new ArrayList<>();
					s1ToS2.add(s2Index);
					
					List<Integer> s2ToS1 = new ArrayList<>();
					s2ToS1.add(s1Index);
					
					graph.put(s1Index, s1ToS2);
					graph.put(s2Index, s2ToS1);
					
					// Add connections from peptide base
					graph.get(connections.get(0))
						.add(s1Index);
					
					if (!graph.containsKey(connections.get(1))) { // second connection is at end of peptide
						List<Integer> target = new ArrayList<>();
						target.add(s2Index);
						
						graph.put(connections.get(1), target);
					}
					else {
						graph.get(connections.get(1))
							.add(s2Index);
					}
					
					// Add connections from disulfide bridge
					graph.get(s1Index)
						.add(connections.get(0));
					graph.get(s2Index)
						.add(connections.get(1));
					
					break;
				case LINEAR:
					break;
			}
		}
		
		return graph;
	}
}
