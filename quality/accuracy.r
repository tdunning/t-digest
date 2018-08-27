data = read.csv("accuracy.csv")
# this makes a nice mark for plotting
data$lx = round(log10(data$q.raw/(1-data$q.raw)))

sizes = read.csv("accuracy-sizes.csv")
sizes$center = with(sizes, (q.0 + q.1)/2)

cdf = read.csv("accuracy-cdf.csv")

buckets = read.csv("accuracy-samples.csv")

plot.cdf = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, add=F, col=rgb(1,0,0,alpha=alpha)) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    boxplot(error ~ q, cdf[i,], add=add, col=col)
}

plot.fill = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(count*compression/1e6/pi*2 ~ center, sizes[i,], pch=21, bg=rgb(0,0,0,alpha=alpha), col=NA, cex=cex)
}

plot.dk = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(dk ~ center, sizes[i,])
}

plot.buckets = function(sortFlag="sorted", alpha = 0.4, clusters=1:7, emphasis=c(), xlim=c(0, 0.003), dx=0.000025, generation=3, digest="MERGE") {
    x.max = 0
    for (ix in clusters) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        x.max = max(c(x.max, zx))
    }
    b = seq(0,1.1 * x.max,by=dx)

    colorIndex = rep(1:4, len=length(clusters))
    colors = c(rgb(1,0,0,alpha=alpha), rgb(0,1,0,alpha=alpha), rgb(0,0,1,alpha=alpha), rgb(0.5, 0.5, 0.5,alpha=alpha))[colorIndex]
    addFlag = F
    for (ix in rev(clusters)) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        hist(zx, xlim=xlim, add=addFlag, breaks=b, col=colors[abs(ix)], border=rgb(0,0,0,alpha=0.1))
        addFlag = T
    }
    for (ix in emphasis) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        data = buckets[i,]$x
        hist(data, add=T, breaks=b, col=colors[ix], border=colors[ix])
    }        
}
