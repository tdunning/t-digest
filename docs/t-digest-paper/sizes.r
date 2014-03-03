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
       ylab='Centroid size', yaxt=yaxt, pch=21, col=rgb(0,0,0,0.2), bg=rgb(0,0,0,0.2))
  title(title)
  box()
  axis(side=1, at=c(0,0.25, 0.5, 0.75, 1), labels=c(0.0,0.25,0.5,0.75, 1.0))

  q = seq(0,1,by=0.01)
  lines(q, 1000*4*q*(1-q), col='gray')
}

pdf("sizes.pdf", width=6, height=2.0, pointsize=9)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.21,1,1))
eldest = par(lwd=0.2)
old = par(mar=c(4.1, 4.1, 2.1, 2))
plotGraph("uniform", title=expression("Uniform Distribution"), T)
par(old)
old = par(mar=c(4.1,0,2.1,2))
plotGraph("gamma", title=expression(paste(Gamma(0.1, 0.1), " Distribution")), F)
plotGraph("sequential", title=expression("Sequential Distribution"), F)
par(old)
par(eldest)
  dev.off()
#plotGraph("mixture", title="Mixture Distribution")
