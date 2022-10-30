fun containsKeyAndValue(map: Map<String, String>, value: String): Boolean {

    var kx = false
    var vx = false

    for ((k, v) in map) {
        if (k == value) {
            kx = true
        }
        if (v == value) {
            vx = true
        }
    }

    return kx && vx
}