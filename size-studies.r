# Experiments with t-digest in R

standard.size.bound = function(n, q) {
    4 * n * q * (1-q)
}

constant.size.bound = function(n, q) {
    n
}

root.size.bound = function(n, q) {
    n * sqrt(4 * q * (1-q))
}

abs.size.bound = function(n, q) {
    2 * n * min(q, 1-q)
}

sorted.t.digest = function (points, compression=50, size.bound = standard.size.bound) {
    points = sort(points)
    n = length(points)

    total = 0
    i = 1
    r = data.frame()
    while (i <= n) {
        # accumulate a centroid of max size
        mean = 0
        count = 0
        qx = total/n
        while (count + 1 <= max(1, do.call(size.bound,list(n=n, q=qx)) / compression)) {
            count = count+1
            mean = mean + (points[i]-mean)/count
            qx = (total + count/2) / n
            i = i+1
        }
        total = total + count
        r = rbind(r, data.frame(center=c(mean), count=c(count)))
    }
    r
}

size.growth = data.frame()
sample.size = c(100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000)
bounds = c("standard.size.bound", "abs.size.bound", "root.size.bound", "constant.size.bound")
for (j in 1:length(bounds)) {
    bound = bounds[j]
    print(bound)
    for (i in 1:length(sample.size)) {
        n = sample.size[i]
        x = rnorm(n)
        cx = sorted.t.digest(x, size.bound=bound)
        size.growth = rbind(size.growth, data.frame(f=bound, n=n, c=dim(cx)[1]))
        print(c(n, dim(cx)[1]))
    }
}

colors = rainbow(3)
colors = c(colors[1],colors[1],colors[2], colors[3])
plot(x=c(), y=c(), xlim=c(1e2,1e6), ylim=c(0,350), log="x", ylab="Centroids", xlab="Points")
for (i in c(1,3,4)) {
  lines(c~n, size.growth[unclass(size.growth$f)==i,], col=colors[i], lwd=2, type='b', cex=0.6)
}
legend(1e2, y=350, legend=c("Standard", "Root", "Constant"), fill=rainbow(3))

direct.t.digest = function (points, compression=50) {
    n = length(points)

    total = 0
    i = 1
    r = data.frame()
    while (i <= n) {
        # accumulate a centroid of max size
        mean = 0
        count = 0
        qx = total
        while (count + 1 <= max(1, 4 * n * (qx * (1-qx) / compression))) {
            count = count+1
            mean = mean + (points[i]-mean)/count
            qx = (total + count/2) / n
            i = i+1
        }
        total = total + count
        r = rbind(r, data.frame(center=c(mean), count=c(count)))
    }
    r
}

    
