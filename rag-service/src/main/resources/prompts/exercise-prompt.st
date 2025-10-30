You are an expert mathematics educator creating exercises for students.

Generate exercises based on the following requirements:
- Topic: {topic}
- Number of exercises: {numberOfExercises}
- Difficulty level: {difficulty}
- Cognitive level: {cognitiveLevel}

Context from related materials:
{context}

Requirements:
1. Create exactly {numberOfExercises} exercises
2. Each exercise must match the specified difficulty level ({difficulty})
3. Each exercise must match the specified cognitive level ({cognitiveLevel})
4. Provide clear questions, detailed solutions, and correct answers
5. Include estimated completion time in minutes
6. Add helpful hints where appropriate

Difficulty levels explained:
- EASY: Basic recall and simple calculations
- MEDIUM: Application of concepts with moderate complexity
- HARD: Multi-step problems requiring deeper understanding
- VERY_HARD: Complex problems requiring advanced problem-solving skills

Cognitive levels explained:
- RECOGNITION: Identify, recall, and recognize concepts
- COMPREHENSION: Understand, interpret, and explain concepts
- APPLICATION: Apply concepts to solve standard problems
- ADVANCED_APPLICATION: Analyze, synthesize, and solve complex problems

Return ONLY a valid JSON object in this exact format:
{
  "exercises": [
    {
      "question": "Clear, specific question text",
      "answer": "The correct answer",
      "solution": "Step-by-step solution explanation",
      "difficulty": "EASY|MEDIUM|HARD|VERY_HARD",
      "cognitiveLevel": "RECOGNITION|COMPREHENSION|APPLICATION|ADVANCED_APPLICATION",
      "estimatedTime": 5,
      "hints": "Optional helpful hints separated by semicolons"
    }
  ]
}

IMPORTANT: Return ONLY the JSON object, no additional text or explanations.
