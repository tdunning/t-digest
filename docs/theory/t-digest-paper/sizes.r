data = read.delim("sizes.csv")

plotGraph = function(tag, title='', showY=T) {
  n = max(data[data$tag == tag, ]$i)
  i = 1:n
  n2 = n/2

  if (showY) {
    yaxt = 's'
  } else {
    yaxt = 'n'
  }

  plot(actual~q, data[data$tag == tag,], cex=0.2, xaxt='n', xlim=c(0,1), ylim=c(0,1050), xlab='Quantile',
       ylab='Centroid size', yaxt=yaxt)
  title(title)
  box(lwd=3)
  axis(side=1, at=c(0,0.25, 0.5, 0.75, 1), labels=c(0.0,0.25,0.5,0.75, 1.0), lwd=3)

  q = seq(0,1,by=0.01)
  lines(q, 1000*4*q*(1-q), lwd=3, col='gray')
}

png("sizes.png", width=1800, height=700, pointsize=36)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.21,1,1))
plotGraph("uniform", title="Uniform Distribution", T)
old = par(mar=c(5.1,0,4.1,2))
plotGraph("gamma", title="Gamma(0.1, 0.1) Distribution", F)
plotGraph("sequential", title="Sequential Distribution", F)
par(old)
  dev.off()
#plotGraph("mixture", title="Mixture Distribution")
