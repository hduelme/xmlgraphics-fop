/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.pdf;

// Java...
import java.util.ArrayList;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.IOException;

/**
 * class representing a PDF Function.
 *
 * PDF Functions represent parameterized mathematical formulas and sampled representations with
 * arbitrary resolution. Functions are used in two areas: device-dependent
 * rasterization information for halftoning and transfer
 * functions, and color specification for smooth shading (a PDF 1.3 feature).
 *
 * All PDF Functions have a FunctionType (0,2,3, or 4), a Domain, and a Range.
 */
public class PDFPattern extends PDFPathPaint {

    /**
     * The resources associated with this pattern
     */
    // Guts common to all function types

    protected PDFResources resources = null;

    /**
     * Either one (1) for tiling, or two (2) for shading.
     */
    protected int patternType = 2;      // Default

    /**
     * The name of the pattern such as "Pa1" or "Pattern1"
     */
    protected String patternName = null;

    /**
     * 1 for colored pattern, 2 for uncolored
     */
    protected int paintType = 2;

    /**
     * 1 for constant spacing, 2 for no distortion, and 3 for fast rendering
     */
    protected int tilingType = 1;

    /**
     * ArrayList of Doubles representing the Bounding box rectangle
     */
    protected ArrayList bBox = null;

    /**
     * Horizontal spacing
     */
    protected double xStep = -1;

    /**
     * Vertical spacing
     */
    protected double yStep = -1;

    /**
     * The Shading object comprising the Type 2 pattern
     */
    protected PDFShading shading = null;

    /**
     * ArrayList of Integers represetning the Extended unique Identifier
     */
    protected ArrayList xUID = null;

    /**
     * TODO use PDFGState
     * String representing the extended Graphics state.
     * Probably will never be used like this.
     */
    protected StringBuffer extGState = null;

    /**
     * ArrayList of Doubles representing the Transformation matrix.
     */
    protected ArrayList matrix = null;

    /**
     * The stream of a pattern
     */
    protected StringBuffer patternDataStream = null;

    /**
     * Create a tiling pattern (type 1).
     *
     * @param theNumber The object number of this PDF Object
     * @param thePatternName The name of the pattern such as "Pa1" or "Pattern1"
     * @param theResources the resources associated with this pattern
     * @param thePatternType the type of pattern, which is 1 for tiling.
     * @param thePaintType 1 or 2, colored or uncolored.
     * @param theTilingType 1, 2, or 3, constant spacing, no distortion, or faster tiling
     * @param theBBox ArrayList of Doubles: The pattern cell bounding box
     * @param theXStep horizontal spacing
     * @param theYStep vertical spacing
     * @param theMatrix Optional ArrayList of Doubles transformation matrix
     * @param theXUID Optional vector of Integers that uniquely identify the pattern
     * @param thePatternDataStream The stream of pattern data to be tiled.
     */
    public PDFPattern(int theNumber, String thePatternName,
                      PDFResources theResources, int thePatternType,    // 1
    int thePaintType, int theTilingType, ArrayList theBBox, double theXStep,
    double theYStep, ArrayList theMatrix, ArrayList theXUID,
    StringBuffer thePatternDataStream) {
        super(theNumber);
        this.patternName = thePatternName;

        this.resources = theResources;
        // This next parameter is implicit to all constructors, and is
        // not directly passed.

        this.patternType = 1;    // thePatternType;
        this.paintType = thePaintType;
        this.tilingType = theTilingType;
        this.bBox = theBBox;
        this.xStep = theXStep;
        this.yStep = theYStep;
        this.matrix = theMatrix;
        this.xUID = theXUID;
        this.patternDataStream = thePatternDataStream;
    }

    /**
     * Create a type 2 pattern (smooth shading)
     *
     * @param theNumber the object number of this PDF object
     * @param thePatternName the name of the pattern
     * @param thePatternType the type of the pattern, which is 2, smooth shading
     * @param theShading the PDF Shading object that comprises this pattern
     * @param theXUID optional:the extended unique Identifier if used.
     * @param theExtGState optional: the extended graphics state, if used.
     * @param theMatrix Optional:ArrayList of Doubles that specify the matrix.
     */
    public PDFPattern(int theNumber, String thePatternName,
                      int thePatternType, PDFShading theShading,
                      ArrayList theXUID, StringBuffer theExtGState,
                      ArrayList theMatrix) {
        super(theNumber);

        this.patternName = thePatternName;

        this.patternType = 2;             // thePatternType;
        this.shading = theShading;
        this.xUID = theXUID;
        // this isn't really implemented, so it should always be null.
        // I just don't want to have to add a new parameter once it is implemented.
        this.extGState = theExtGState;    // always null
        this.matrix = theMatrix;
    }

    /**
     * Get the name of the pattern
     *
     * @return String representing the name of the pattern.
     */
    public String getName() {
        return (this.patternName);
    }

    public String getColorSpaceOut(boolean fillNotStroke) {
        if (fillNotStroke) {    // fill but no stroke
            return ("/Pattern cs /" + this.getName() + " scn \n");
        } else {                // stroke (or border)
            return ("/Pattern CS /" + this.getName() + " SCN \n");
        }
    }


    /**
     * represent as PDF. Whatever the FunctionType is, the correct
     * representation spits out. The sets of required and optional
     * attributes are different for each type, but if a required
     * attribute's object was constructed as null, then no error
     * is raised. Instead, the malformed PDF that was requested
     * by the construction is dutifully output.
     * This policy should be reviewed.
     *
     * @return the PDF string.
     */
    protected int output(OutputStream stream) throws IOException {

        int vectorSize = 0;
        int tempInt = 0;
        StringBuffer p = new StringBuffer();
        p.append(this.number + " " + this.generation
                 + " obj\n<< \n/Type /Pattern \n");

        if (this.resources != null) {
            p.append("/Resources " + this.resources.referencePDF() + " \n");
        }

        p.append("/PatternType " + this.patternType + " \n");

        PDFStream dataStream = null;

        if (this.patternType == 1) {
            p.append("/PaintType " + this.paintType + " \n");
            p.append("/TilingType " + this.tilingType + " \n");

            if (this.bBox != null) {
                vectorSize = this.bBox.size();
                p.append("/BBox [ ");
                for (tempInt = 0; tempInt < vectorSize; tempInt++) {
                    p.append(PDFNumber.doubleOut((Double)this.bBox.get(tempInt)));
                    p.append(" ");
                }
                p.append("] \n");
            }
            p.append("/XStep " + PDFNumber.doubleOut(new Double(this.xStep))
                     + " \n");
            p.append("/YStep " + PDFNumber.doubleOut(new Double(this.yStep))
                     + " \n");

            if (this.matrix != null) {
                vectorSize = this.matrix.size();
                p.append("/Matrix [ ");
                for (tempInt = 0; tempInt < vectorSize; tempInt++) {
                    p.append(PDFNumber.doubleOut((Double)this.matrix.get(tempInt)));
                    p.append(" ");
                }
                p.append("] \n");
            }

            if (this.xUID != null) {
                vectorSize = this.xUID.size();
                p.append("/XUID [ ");
                for (tempInt = 0; tempInt < vectorSize; tempInt++) {
                    p.append(((Integer)this.xUID.get(tempInt)) + " ");
                }
                p.append("] \n");
            }

            // don't forget the length of the stream.
            if (this.patternDataStream != null) {
                dataStream = new PDFStream(0);
                dataStream.add(this.patternDataStream.toString());
                // TODO get the filters from the doc
                dataStream.addDefaultFilters(new HashMap(), PDFStream.CONTENT_FILTER);
                p.append(dataStream.applyFilters());
                p.append("/Length " + (dataStream.getDataLength() + 1)
                         + " \n");
            }

        } else    // if (this.patternType ==2)
         {        // Smooth Shading...
            if (this.shading != null) {
                p.append("/Shading " + this.shading.referencePDF() + " \n");
            }

            if (this.xUID != null) {
                vectorSize = this.xUID.size();
                p.append("/XUID [ ");
                for (tempInt = 0; tempInt < vectorSize; tempInt++) {
                    p.append(((Integer)this.xUID.get(tempInt)) + " ");
                }
                p.append("] \n");
            }

            if (this.extGState != null) {
                p.append("/ExtGState " + this.extGState + " \n");
            }

            if (this.matrix != null) {
                vectorSize = this.matrix.size();
                p.append("/Matrix [ ");
                for (tempInt = 0; tempInt < vectorSize; tempInt++) {
                    p.append(PDFNumber.doubleOut((Double)this.matrix.get(tempInt)));
                    p.append(" ");
                }
                p.append("] \n");
            }
        }         // end of if patterntype =1...else 2.

        p.append(">> \n");

        String dict = p.toString();
        int length = dict.length();

        stream.write(dict.getBytes());

        // stream representing the function
        if (dataStream != null) {
            length += dataStream.outputStreamData(stream);
        }

        String end = "endobj\n";
        stream.write(end.getBytes());
        length += end.length();


        return length;

    }

    public byte[] toPDF() { return null; }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PDFPattern)) {
            return false;
        }
        PDFPattern patt = (PDFPattern)obj;
        if (patternType != patt.patternType) {
            return false;
        }
        if (paintType != patt.paintType) {
            return false;
        }
        if (tilingType != patt.tilingType) {
            return false;
        }
        if (xStep != patt.xStep) {
            return false;
        }
        if (yStep != patt.yStep) {
            return false;
        }
        if (bBox != null) {
            if (!bBox.equals(patt.bBox)) {
                return false;
            }
        } else if (patt.bBox != null) {
            return false;
        }
        if (bBox != null) {
            if (!bBox.equals(patt.bBox)) {
                return false;
            }
        } else if (patt.bBox != null) {
            return false;
        }
        if (xUID != null) {
            if (!xUID.equals(patt.xUID)) {
                return false;
            }
        } else if (patt.xUID != null) {
            return false;
        }
        if (extGState != null) {
            if (!extGState.equals(patt.extGState)) {
                return false;
            }
        } else if (patt.extGState != null) {
            return false;
        }
        if (matrix != null) {
            if (!matrix.equals(patt.matrix)) {
                return false;
            }
        } else if (patt.matrix != null) {
            return false;
        }
        if (resources != null) {
            if (!resources.equals(patt.resources)) {
                return false;
            }
        } else if (patt.resources != null) {
            return false;
        }
        if (shading != null) {
            if (!shading.equals(patt.shading)) {
                return false;
            }
        } else if (patt.shading != null) {
            return false;
        }
        if (patternDataStream != null) {
            if (!patternDataStream.equals(patt.patternDataStream)) {
                return false;
            }
        } else if (patt.patternDataStream != null) {
            return false;
        }

        return true;
    }

}
