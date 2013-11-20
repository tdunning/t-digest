png("k-approx.png", width=1800, height=1800, pointsize=72)
data = read.delim("sizes.csv")

plotGraph = function(tag, title='') {
  n = max(data[data$tag == tag, ]$i)
  i = 1:n
  n2 = n/2

  png(paste(tag, "-sizes.png", sep=''), width=1800, height=1800, pointsize=72)
  plot(actual~q, data[data$tag == tag,], cex=0.2, xaxt='n', xlim=c(0,1), ylim=c(0,1050), xlab='Cluster', ylab='Centroid size')
  title(title)
  box(lwd=3)
  axis(side=1, at=c(0,0.25, 0.5, 0.75, 1), labels=c(0.0,0.25,0.5,0.75, 1.0), lwd=3)

  q = seq(0,1,by=0.01)
  lines(q, 1000*4*q*(1-q), lwd=9)
  dev.off()
}

plotGraph("gamma", title="Gamma(0.1, 0.1) Distribution")
plotGraph("uniform", title="Uniform Distribution")
plotGraph("mixture", title="Mixture Distribution")
