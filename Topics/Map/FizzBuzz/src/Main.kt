fun iterator(map: Map<String, Int>) {

    for (p in map) {
        if (p.value % 3 == 0) println("Fizz")
        else if (p.value % 5 == 0) println("Buzz")
        else println("FizzBuzz")
    }
}