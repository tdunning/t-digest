expt = function(x) {
    if (length(x) > 1) {
        sapply(x, expt)
    } else {
        data = readBin(writeBin(x, raw(8)), "int", n=2)
        r = bitwShiftR(bitwAnd(data[2], 0x3ff00000), 20)
        if (r >= 512) {
            r = r-1024
        }
        return(r+1)
    }
}

mantissa = function(x) {
    if (length(x) > 1) {
        sapply(x, mantissa)
    } else {
        data = readBin(writeBin(x, raw(8)), "int", n=2)
        data[2] = bitwOr(bitwAnd(data[2], -0x7ff00001), 0x3ff00000)
        readBin(writeBin(data, raw(8)), "double")
    }
}

approxLog2 = function(x) {
    m = mantissa(x)
    expt(x) + ((6 * m - m * m) - 5)/3
}
