(ns calendar.png
  "Bake data into PNGs."
  (:import (java.io File)
           (com.sun.imageio.plugins.png PNGMetadata)
           (java.awt.image RenderedImage)
           (javax.imageio ImageIO IIOImage)
           (javax.imageio.stream FileImageOutputStream)))

(defn set-metadata
  "Set a metadata field on a PNG, creating metadata if needed."
  [^IIOImage image field-name data-string]
  (if (nil? (.getMetadata image))
    (.setMetadata image (PNGMetadata.)))
  (let [metadata (.getMetadata image)]
    (.add (.tEXt_keyword metadata) field-name)
    (.add (.tEXt_text metadata) data-string))
  image)

(defn write-image
  "Write the image to a file"
  [output-file-name ^RenderedImage image]
  (let [writer (.next (ImageIO/getImageWritersBySuffix "png"))]
     (.setOutput writer (FileImageOutputStream. (File. output-file-name)))
     (.write writer nil image nil)))

(defn bake
  "Bake keywords into an image"
  [original-image-file output-image-file [field-name data-string]]
  (let [image (IIOImage. (ImageIO/read (File. original-image-file)) nil nil)]
    (write-image output-image-file (set-metadata image field-name data-string))))