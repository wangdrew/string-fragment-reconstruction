import java.util.HashMap;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.PriorityQueue;

public class ReconstructFragments {
	
	/*
	 * Running time analysis:
	 * 
	 * Priority Queue version for constant time max overlap access:
	 * 
	 * Compute all fragment overlap lengths and store in priority queue - O((nlogn)^2)
	 * For every merge - O(n) * {
	 * 	 Get the maximum overlap pair - O(c)
	 * 	 Merge - O(c2) - quadratic time (worst-case), linear time (best) dependent on length of string, 
	 *                   show as constant time because string lengths are < 1200 chars
	 *   Create the new fragment, store in hashtable - O(c)
	 *   Delete any pairs containing the merge fragments from the priority queue - O(nlogn)
	 *   Recreate new pairs containing the merged fragment, insert into priority queue - O(nlogn)
	 * }
	 * 
	 * Total = O((nlogn)^2) + O(n) * { O(c) + O(nlogn)} = O(nlogn)^2) + O(n) + O(n^2 * logn) = O(nlogn)^2 
	 * 
	 * 
	 * 
	 * Brute Force version which recomputes all overlap lengths for every fragment merge:
	 * 
	 * For every merge - O(n) * {
	 *    Find the maximum overlap pair - O(n^2)
	 *    Merge - O(c2)
	 * }
	 * 
	 * Total = O(n) * O(n^2) = O(n^3) 
	 * 
	 */
	
	/**
	 * 
	 * @param args
	 */	
	public static void main(String[] args) {
		
		long begin = System.nanoTime();
		if (reconstructFragmentsBruteForce() == -1) {
			System.out.println("Error reconstructing message");
		}
		long end = System.nanoTime();
		double time = (end-begin)/Math.pow(10,9);
		System.out.println("Brute force running time: " + time + " s"); 
		
		
		begin = System.nanoTime();
		if (reconstructFragmentsHeap() == -1) {
			System.out.println("Error reconstructing message");
		}
		end = System.nanoTime();
		time = (end-begin)/Math.pow(10,9);
		System.out.println("Heap method running time: " + time + " s"); 
		
	}
	
	/**
	 * Merges the string fragments using a priority queue for
	 * constant time access to the fragment pair with the largest overlap
	 * Once the maximal pair is retrieved, any other fragment pair that overlap and 
	 * contain a fragment in the maximal pair gets the overlap length recomputed
	 * with the newly merged fragment. This recomputed pair is re-inserted into 
	 * the priority queue. This process continues until all of the fragments are merged
	 * 
	 * @return errorCode: 0 if success, >0 if error
	 */
	public static int reconstructFragmentsHeap() {
		
		PriorityQueue<FragmentPair> orderedPairs = new PriorityQueue<FragmentPair>();
		HashMap<Integer,String> fragments = parseInput();
		int newIDSeed = fragments.size();
		
		// Initial computation of overlap lengths for all possible fragment pairs
		for (int fragA = 0; fragA < fragments.size(); fragA++) {
			for (int fragB = fragA + 1; fragB < fragments.size(); fragB++) {
				
				// Determine the overlap length of fragment A and B
				int overlapLength = 
						determineOverlap(fragments.get(fragA),fragments.get(fragB)).get(0);
				
				// If there is an overlap, insert it into priority queue
				if (overlapLength > 0) {
					orderedPairs.add(new FragmentPair(overlapLength,fragA,fragB));
				}
			}	
		}

		// Keep merging while there are still fragments to be merged
        // TODO: Ideal implementation would check to make sure that
        // there are overlapping regions in the remaining fragments and return
        // an error if there are not.
		while (fragments.size() > 1) {
			
			// Get the pair with the largest overlap
			FragmentPair overlapPair =  orderedPairs.poll();
			if (overlapPair == null) return -1;
			int old_a_id = overlapPair.a_id;
			int old_b_id = overlapPair.b_id;
			int newFragmentID = newIDSeed++;
			
			// Merge the two fragments
			String mergedFragments = mergeStrings(fragments.get(old_a_id), fragments.get(old_b_id));
			
			// Add the merged fragment to the hashmap
			fragments.put(newFragmentID, mergedFragments);
			
			// Remove the old fragments from the hashmap
			fragments.remove(old_a_id);
			fragments.remove(old_b_id);

			// This set contains the fragments that previously paired with the old fragments
			HashSet<Integer> previousPair = new HashSet<Integer>(); 
			
			// This set contains pairs that will be deleted because they contain an old fragment
			HashSet<FragmentPair> pairsToDelete = new HashSet<FragmentPair>(); 
			
			// Mark any pair that has an old fragment for deletion
			Iterator<FragmentPair> iter = orderedPairs.iterator();
			while (iter.hasNext()) {
				FragmentPair pair = iter.next();
				if (pair.containsFragment(old_a_id) && pair.containsFragment(old_b_id)) {
					pairsToDelete.add(pair);
				} else if (pair.a_id == old_a_id || pair.a_id == old_b_id) {
					previousPair.add(pair.b_id);
					pairsToDelete.add(pair);
				} else if (pair.b_id == old_a_id || pair.b_id == old_b_id) {
					previousPair.add(pair.a_id);
					pairsToDelete.add(pair);
				}
			}
			
			// Delete the pairs
			Iterator<FragmentPair> iter2 = pairsToDelete.iterator();
			while (iter2.hasNext()) {
				orderedPairs.remove(iter2.next());
			}
			
			// Recompute new overlap lengths that contain the merged fragment and the 
			// fragments that previously paired with one of the old fragments
			Iterator<Integer> iter3 = previousPair.iterator();
			while (iter3.hasNext()) {
				int new_b_id = iter3.next();
				int overlapLength = determineOverlap(fragments.get(newFragmentID), 
						fragments.get(new_b_id)).get(0);
				
				// Put the new pair in the priority queue
				orderedPairs.add(new FragmentPair(overlapLength,newFragmentID,new_b_id));
			}
		}
		System.out.println(fragments.get(fragments.keySet().toArray()[0])); 
		return 0;
	} 	
	
    /**
	 * Recomputes overlap lengths for all possible fragment pairs for every
	 * merge of the maximal overlap fragment pair
	 * @return errorCode: 0 if success, >0 if error
	 */
	public static int reconstructFragmentsBruteForce() {
		HashMap<Integer,String> fragments = parseInput();
		
		int maxOverlap = Integer.MIN_VALUE;
		int localOverlap = 0;
		int stringA = -1;
		int stringB = -1;
		int newKeyStart = fragments.size();
		
        // Keep merging while there are still fragments to be merged
		while (fragments.size() > 1) {
			
			Integer[] keys = 
                fragments.keySet().toArray(new Integer[fragments.keySet().size()]);
            
            // Recompute all overlap lengths for all possible fragment pairs
			for (int i = 0; i < keys.length; i++) {
				for (int j = i + 1; j < keys.length; j++) {
					localOverlap = 
                        determineOverlap(fragments.get(keys[i]), 
                                         fragments.get(keys[j]) ).get(0);
					
                    // Find the maximal overlap length
                    if (localOverlap > maxOverlap) {
						stringA = keys[i];
						stringB = keys[j];
						maxOverlap = localOverlap;
					}
				}
			}
            
            // Merge the two strings with the maximal overlap
			String result = mergeStrings(fragments.get(stringA),fragments.get(stringB));
			fragments.put(newKeyStart++, result);
			fragments.remove(stringA);
			fragments.remove(stringB);
			localOverlap = 0;
			maxOverlap = Integer.MIN_VALUE;
		}
		
        // Print the last fragment out to STDOUT
		Integer[] keys = fragments.keySet().toArray(new Integer[fragments.keySet().size()]);
		System.out.println(fragments.get(keys[0])); 
		return 0;
	}
	
	/**
	 * Parses string fragments from STDIN using semicolon char as delimiter.
	 * @return	HashMap with the string fragment's order of appearance as the key
	 */
	public static HashMap<Integer,String> parseInput() {
		
		// Store all stdin fragments into a hashmap with the key as 
		// the fragment's order of appearance
		HashMap<Integer,String> inputStrings = new HashMap<Integer,String>();
		//Scanner stdin = new Scanner(System.in);
		File file = new File("./samples/input001.txt");
		
		try {
			Scanner stdin = new Scanner(file);
			int index = 0;
			
			String line = stdin.nextLine();
			
	        // Use semicolon as delimiter 
			String[] tokens = line.split(";");
			
			for (String s: tokens) {
				inputStrings.put(index++, s);
			}
			
			stdin.close();
			
		} catch (FileNotFoundException e){
			System.out.println("File not found");
		}
		return inputStrings;
		
		/*
		// Store all stdin fragments into a hashmap with the key as 
		// the fragment's order of appearance
 		HashMap<Integer,String> inputStrings = new HashMap<Integer,String>();
		Scanner stdin = new Scanner(System.in);
		
        int index = 0;
        
        String line = stdin.nextLine();
        
        // Use semicolon as delimiter 
        String[] tokens = line.split(";");
        
        for (String s: tokens) {
            inputStrings.put(index++, s);
        }
        
        stdin.close();
			
		return inputStrings;
	
		 */
	
	}
	
	
	/**
	 * Determines the overlap character length between two strings and the
     * overlapping region start and end in both strings.
     *
     * @params a	First String
     * @params b	Second String
	 * @return	ArrayList with the following format:
	 * 			[overlap length, seq start in A (incl), seq end in A (excl), 
	 *           seq start in B (incl), seq end in B (excl)]
	 */
	public static ArrayList<Integer> determineOverlap(String a, String b) {
		int maxLength = Integer.MIN_VALUE;
		int localBeginA = 0, localBeginB = 0;
		int beginA = 0, endA = 0, beginB = 0, endB = 0;
		int localLength = 0;
		char[] aChar = a.toCharArray();
		char[] bChar = b.toCharArray();
		
		// Try to match the first character in B with any character in A
		// This searches for a matching sequence that starts in the middle of A and starts or encompasses all of B
		localBeginB = 0;
		for (int i = 0; i < aChar.length; i++) {
			
			// Found a matching character in A!
			if (bChar[0] == aChar[i]) {
				int j_p = 0;
				int i_p = i;
				localBeginA = i;
				
				// Now iterate through both A and B to see if subsequent characters match
				while (i_p < aChar.length && j_p < bChar.length &&
						aChar[i_p] == bChar[j_p]) {
					localLength++;
					i_p++;
					j_p++;
				}
				
				// After we discover the matching sequence:
				// If the matching sequence is in the middle of both A and B strings,
				// disregard the match, there is no overlap.
				if (j_p < bChar.length && i_p < aChar.length && localLength > 0) {
					localBeginA = 0;
				} 
				// Otherwise if this is the longest sequence we've seen,
				// this must be the actual overlapping region.
				// Save the start & end for both A and B
				else if (localLength > maxLength) {
					beginA = localBeginA;
					beginB = localBeginB;
					endA = i_p;
					endB = j_p;
					
					maxLength = localLength;
				}
				
				localLength = 0;
			}
		}
		
		// Try to match the first character in A with any character in B
		// This searches for a matching sequence that starts in the middle of B and starts or encompasses all of A
		localBeginA = 0;
		for (int j = 0; j < bChar.length; j++) {
			
			// Found a matching character in B!
			if (bChar[j] == aChar[0]) {
				int j_p = j;
				int i_p = 0;
				localBeginB = j;
				
				// Now iterate through both A and B to see if subsequent characters match
				while (i_p < aChar.length && j_p < bChar.length &&
						aChar[i_p] == bChar[j_p]) {
					localLength++;
					i_p++;
					j_p++;
				}
				
				// After we discover the matching sequence:
				// If the matching sequence is in the middle of both A and B strings,
				// disregard the match, there is no overlap.
				if (j_p < bChar.length && i_p < aChar.length && localLength > 0) { 
					localBeginB = 0;				
				} 
				
				// Otherwise if this is the longest sequence we've seen,
				// this must be the actual overlapping region.
				// Save the start & end for both A and B
				else if (localLength > maxLength) {
					beginA = localBeginA;
					beginB = localBeginB;
					endA = i_p;
					endB = j_p;
					
					maxLength = localLength;
				}
				
				localLength = 0;
			}
		}
		
		if (maxLength < 0) maxLength = 0;
		
		// Construct the output
		// Format: [sequence length, A seq start(incl), A seq end(excl),  B seq start (incl), B seq end (excl)]
		ArrayList<Integer> solution = new ArrayList<Integer>();
		solution.add(maxLength);
		solution.add(beginA);
		solution.add(endA);
		solution.add(beginB);
		solution.add(endB);
		return solution;
	}
	
	
	/**
	 * Merges two Strings depending on region of overlap 
	 * and returns single result String
	 * @param a 	first String in String pair to be merged
	 * @param b 	second String in String pair to be merged
	 * @return 		merged output of String a and b
	 */
	public static String mergeStrings(String a, String b) {
		
		// [length, A begin incl, A end excl, B begin incl, B end excl]
		ArrayList<Integer> overlapBoundary = determineOverlap(a,b);
		//System.out.println(overlapBoundary);
		String result = null;
		
		// First check to see if there is an overlap
		if (overlapBoundary.get(0) != 0) {
			
			// B is the entire overlapped region
			if (overlapBoundary.get(3) == 0 && overlapBoundary.get(4) == b.length()) {
				result = a;
			}
			// A is the entire overlapped region
			else if (overlapBoundary.get(1) == 0 && overlapBoundary.get(2) == a.length()) {
				result = b;
			}
			// Overlapping region begins in A and ends in B
			else if (overlapBoundary.get(2) == a.length() && 
						overlapBoundary.get(3) == 0 && 
						overlapBoundary.get(4) > 0) {
				result = new String(a.substring(0,overlapBoundary.get(1)) + b);
			}
			// Overlapping region begins in B and ends in A
			else if (overlapBoundary.get(4) == b.length() &&
						overlapBoundary.get(1) == 0 &&
						overlapBoundary.get(2) > 0) {
				result = new String(b.substring(0,overlapBoundary.get(3)) + a);
			}
			
			// Something went wrong, return empty string
			else {
				result = "";
			}
		}
		
		// No overlap, return an empty string
		else {
			result = "";
		}

		return result;
	}
}

/**
 * Private class used to represent a fragment pair 
 * and the length of any potential overlap.
 * Fragments are represented by Integer ID values.
 *
 */
class FragmentPair implements Comparable<FragmentPair>{

	public int overlapLength;
	public int a_id;
	public int b_id;
	
	/**
	 * 
	 * @param overlap_length	number of characters a and b fragments overlap
	 * @param a_fragment		integer ID of fragment a
	 * @param b_fragment		integer ID of fragment b
	 */
	FragmentPair(int overlap_length, int a_fragment, int b_fragment) {
		overlapLength = overlap_length;
		a_id = a_fragment;
		b_id = b_fragment;
	}
	/**
	 * 
	 * @return fragment A ID
	 */
	public int getfragmentA() { 
		return a_id;
	}
	/**
	 * 
	 * @return fragment B ID
	 */
	public int getfragmentB() {
		return b_id;
	}
	
	/**
	 * 
	 * @param id ID of fragment
	 * @return boolean value of whether input fragment ID exists in this pair
	 */
	public boolean containsFragment(int id) {
		return ( (id == a_id) || (id == b_id) );
	}
	
	/**
	 * 
	 * @param f fragment pair to compare to
	 * @returns integer value of the amount of difference between
	 * input fragment pair overlap and this fragment pair overlap.
	 */
	@Override
	public int compareTo(FragmentPair f) {
		return f.overlapLength - this.overlapLength;
	}
	
}


// ===========================================================

/* Code graveyard 
 
 Previous comments for determineSequence
 	// TODO: Needs unit-testing
	// This function has an O(c) running time 
	// because the maximum string length is guaranteed to be <1200
	
	// Smarter method below:
	// Try to match first character of a with matching character in b
	// Try to match character in a with the first character in b
	// Try to match a sequence in a with a sequence in b
	// Assumes that sequences can't be arbitrarily placed in another sequence
	// i.e. "on" in "on and on" 
 
 
 
 	For printing out the fragments:
 			for (int i = 0; i < fragments.size(); i++) {
			System.out.print(i + ": ");
			System.out.println(fragments.get(i));
		}
 
	 Priorityqueue unit test: 
			
	PriorityQueue<FragmentPair> fragmentPairs = new PriorityQueue<FragmentPair>(); 
	FragmentPair test = new FragmentPair(10,4,3);
	FragmentPair test2 = new FragmentPair(8,4,3);
	FragmentPair test3 = new FragmentPair(12,4,3);
	fragmentPairs.add(test);
	fragmentPairs.add(test2);
	fragmentPairs.add(test3);
	System.out.println(fragmentPairs.poll().overlapLength);
	System.out.println(fragmentPairs.poll().overlapLength);
	System.out.println(fragmentPairs.poll().overlapLength);
 
 	public static PriorityQueue<PriorityQueue<Integer>> constructLCSLengths() {
		PriorityQueue<PriorityQueue<Integer>> CommonSubstringPairs = new
				PriorityQueue<PriorityQueue<Integer>>();
		
		// For every string fragment
		for (int i = 0; i < fragments.size(); i++) {
			PriorityQueue<Integer> CommonSequencePairs = new PriorityQueue<Integer>();
			
			// Go through all other string fragments and determine the common subsequence length
			for (int j = i; j < fragments.size(); j++) {
				determineOverlap(fragments.get(i), fragments.get(j));
				
			}
			CommonSubstringPairs.add(CommonSequencePairs);
			fragments.get(i);
		}
	}
	
	
 
 		System.out.println(mergeStrings("greatpekingduckfordinner","greatpeking"));
		System.out.println(mergeStrings("greatpeking","greatpekingduckfordinner"));
		System.out.println(mergeStrings("greatpeking","pekingduckfordinner"));
		System.out.println(mergeStrings("pekingduckfordinner","greatpeking"));
		System.out.println(mergeStrings("duck","pekingduckfordinner"));
		System.out.println(mergeStrings("pekingduckfordinner","duck"));
		System.out.println(mergeStrings("pekingduckfordinner","pekingduckfordinner"));
		
		
 
 Brute force LCS length method below

public static int determineOverlap(String a, String b) {
	
	// Determine Longest Common Substring length
	int maxCommonLen = Integer.MIN_VALUE;
	int localCommonLen = 0;
	char[] aChar = a.toCharArray();
	char[] bChar = b.toCharArray();
	
	for (int i = 0; i < aChar.length; i++) {
		for (int j = 0; j < bChar.length; j++) {
			if (aChar[i] == bChar[j]) {
				int i_p = i;
				int j_p = j;
				while (i_p < aChar.length && j_p < bChar.length &&
						aChar[i_p] == bChar[j_p]) {
					i_p++;
					j_p++;
					localCommonLen++;
				}
				
				if (localCommonLen > maxCommonLen) { 
					maxCommonLen = localCommonLen;
				}
				
				localCommonLen = 0;
			}
			
		}
	}
	return maxCommonLen;
}
*/
