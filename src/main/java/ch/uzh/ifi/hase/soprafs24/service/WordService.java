package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

@Service
public class WordService {
    private final Random random = new Random();
    
    // List of words that are good for the Chameleon game
    private final List<String> wordList = Arrays.asList(
        "apple", "banana", "orange", "grape", "strawberry",
        "computer", "keyboard", "mouse", "monitor", "laptop",
        "book", "pencil", "paper", "desk", "chair",
        "dog", "cat", "bird", "fish", "rabbit",
        "car", "bike", "bus", "train", "plane",
        "house", "apartment", "building", "room", "garden",
        "pizza", "burger", "sandwich", "salad", "soup",
        "movie", "music", "game", "sport", "art",
        "sun", "moon", "star", "cloud", "rain",
        "beach", "mountain", "forest", "river", "lake"
    );

    /**
     * Gets a random word from the word list
     * @return A random word
     */
    public String getRandomWord() {
        int randomIndex = random.nextInt(wordList.size());
        return wordList.get(randomIndex);
    }
} 