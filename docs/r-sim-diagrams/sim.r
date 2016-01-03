x = seq(-5,5,by=0.01)
sizeLimit = function(n,x) {q=pnorm(x);4*sum(counts)*q*(1-q)/50}
addPoint = function(p) {
    dist = abs(centroids - p)
    k = which(min(dist) == dist)[1]
    if (counts[k] < sizeLimit(N, p)) {
        counts[k] <<- counts[k]+1
        centroids[k] <<- centroids[k] + (p-centroids[k]) / counts[k]
    } else {
        centroids <<- c(centroids, p)
        counts <<- c(counts, 1)
    }
}

offset = 100
step = function(n=100) {
    for (i in 1:n) {
        addPoint(samples[i+offset])
    }
    offset <<- offset + n
    counts
}

plot(x, sizeLimit(40, x), type='l')

N = 10e6
samples = rnorm(N)
centroids = samples[1:2]
counts = (centroids != 1000) + 0

plot.offset = 0
plot.stuff = function(stepSize=1000) {
    for (i in 1:10) {
        step(stepSize)
        pdf(sprintf("fig-%03d.pdf", i + plot.offset),
            width=5, height=5, pointsize=10)
        plot(pnorm(centroids[order(centroids)]), counts[order(centroids)],
             type='p', ylim=c(0, 2*max(counts)),
             pch=21, bg=rgb(0,0,0,alpha=0.1), col=rgb(0,0,0,alpha=0.1), cex=0.6);
        centers = centroids[order(centroids)]
        limits = sizeLimit(sum(counts), centers)
        lines(pnorm(centers), limits, type='l')
        dev.off()
    }
    plot.offset <<- plot.offset + 10
}

plot.stuff(100)
plot.stuff(1000)
plot.stuff(10000)
plot.stuff(100000)
