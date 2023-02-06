/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2023 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.forms.fields;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.source.PdfTokenizer;
import com.itextpdf.io.source.RandomAccessFileOrArray;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceCmyk;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfObjectWrapper;
import com.itextpdf.kernel.pdf.PdfString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a single field or field group in an {@link com.itextpdf.forms.PdfAcroForm
 * AcroForm}.
 *
 * <p>
 * To be able to be wrapped with this {@link PdfObjectWrapper} the {@link PdfObject}
 * must be indirect.
 */
public abstract class AbstractPdfFormField extends PdfObjectWrapper<PdfDictionary> {

    /**
     * Size of text in form fields when font size is not explicitly set.
     */
    static final int DEFAULT_FONT_SIZE = 12;

    /**
     * Minimal size of text in form fields.
     */
    static final int MIN_FONT_SIZE = 4;

    /**
     * Index of font value in default appearance element.
     */
    private static final int DA_FONT = 0;

    /**
     * Index of font size value in default appearance element.
     */
    private static final int DA_SIZE = 1;

    /**
     * Index of color value in default appearance element.
     */
    private static final int DA_COLOR = 2;

    private static final Set<PdfName> formFieldKeys = new HashSet<PdfName>();

    protected PdfFont font;
    protected float fontSize = -1;
    protected Color color;
    protected PdfAConformanceLevel pdfAConformanceLevel;

    /**
     * Parent form field.
     */
    protected PdfFormField parent;

    /**
     * Creates a form field as a wrapper object around a {@link PdfDictionary}.
     * This {@link PdfDictionary} must be an indirect object.
     *
     * @param pdfObject the dictionary to be wrapped, must have an indirect reference.
     */
    protected AbstractPdfFormField(PdfDictionary pdfObject) {
        super(pdfObject);
        ensureObjectIsAddedToDocument(pdfObject);
        setForbidRelease();
        retrieveStyles();
    }

    /**
     * Gets the wrapped dictionary.
     *
     * @return the wrapped dictionary.
     */
    @Override
    public PdfDictionary getPdfObject() {
        return super.getPdfObject();
    }

    /**
     * Sets a parent {@link PdfFormField} for the current object.
     *
     * @param parent another form field that this field belongs to, usually a group field.
     */
    public void setParent(PdfFormField parent) {
        if (!parent.getPdfObject().equals(this.getParent()) && !parent.getPdfObject().equals(this.getPdfObject())) {
            put(PdfName.Parent, parent.getPdfObject());
        }
        this.parent = parent;
    }

    /**
     * Gets the parent dictionary.
     *
     * @return another form field that this field belongs to.
     */
    public PdfDictionary getParent() {
        PdfDictionary parentDict = getPdfObject().getAsDictionary(PdfName.Parent);
        if (parentDict == null) {
            parentDict = parent == null ? null : parent.getPdfObject();
        }
        return parentDict;
    }

    /**
     * Gets the parent field.
     *
     * @return another form field that this field belongs to.
     */
    public PdfFormField getParentField() {
        return this.parent;
    }

    /**
     * Gets the current field name.
     *
     * @return the current field name, as a {@link PdfString}.
     */
    public PdfString getFieldName() {
        String parentName = "";
        PdfDictionary parentDict = getParent();
        if (parentDict != null) {
            PdfFormField parentField = getParentField();
            if (parentField == null) {
                parentField = PdfFormField.makeFormField(getParent(), getDocument());
            }
            PdfString pName = parentField.getFieldName();
            if (pName != null) {
                parentName = pName.toUnicodeString() + ".";
            }
        }
        PdfString name = getPdfObject().getAsString(PdfName.T);
        if (name != null) {
            name = new PdfString(parentName + name.toUnicodeString(), PdfEncodings.UNICODE_BIG);
        }
        return name;
    }

    /**
     * Gets default appearance string containing a sequence of valid page-content graphics or text state operators that
     * define such properties as the field's text size and color.
     *
     * @return the default appearance graphics, as a {@link PdfString}.
     */
    public abstract PdfString getDefaultAppearance();

    /**
     * Gets the current fontSize of the form field.
     *
     * @return the current fontSize.
     */
    public float getFontSize() {
        float fontSizeToReturn = fontSize == -1 && parent != null ? parent.getFontSize() : fontSize;
        if (fontSizeToReturn == -1) {
            fontSizeToReturn = DEFAULT_FONT_SIZE;
        }
        return fontSizeToReturn;
    }

    /**
     * Gets the current font of the form field.
     *
     * @return the current {@link PdfFont font}
     */
    public PdfFont getFont() {
        PdfFont fontToReturn = font == null && parent != null ? parent.getFont() : font;
        if (fontToReturn == null) {
            fontToReturn = getDocument().getDefaultFont();
        }
        return fontToReturn;
    }

    /**
     * Gets the current color of the form field.
     *
     * @return the current {@link Color color}
     */
    public Color getColor() {
        return color == null && parent != null ? parent.getColor() : color;
    }

    /**
     * Gets the declared PDF/A conformance level.
     *
     * @return the {@link PdfAConformanceLevel}
     */
    public PdfAConformanceLevel getPdfAConformanceLevel() {
        return pdfAConformanceLevel == null && parent != null ? parent.getPdfAConformanceLevel() : pdfAConformanceLevel;
    }

    /**
     * This method regenerates appearance stream of the field. Use it if you
     * changed any field parameters and didn't use setValue method which
     * generates appearance by itself.
     *
     * @return whether or not the regeneration was successful.
     */
    public abstract boolean regenerateField();

    /**
     * Sets the text color and does not regenerate appearance stream.
     *
     * @param color the new value for the Color.
     * @return the edited field.
     */
    void setColorNoRegenerate(Color color) {
        this.color = color;
    }

    /**
     * Gets the appearance state names.
     *
     * @return an array of Strings containing the names of the appearance states.
     */
    public abstract String[] getAppearanceStates();

    /**
     * Inserts the value into the {@link PdfDictionary} of this field and associates it with the specified key.
     * If the key is already present in this field dictionary,
     * this method will override the old value with the specified one.
     *
     * @param key  key to insert or to override.
     * @param value the value to associate with the specified key.
     * @return the edited field.
     */
    public AbstractPdfFormField put(PdfName key, PdfObject value) {
        getPdfObject().put(key, value);
        setModified();
        return this;
    }

    /**
     * Removes the specified key from the {@link PdfDictionary} of this field.
     *
     * @param key key to be removed.
     * @return the edited field.
     */
    public AbstractPdfFormField remove(PdfName key) {
        getPdfObject().remove(key);
        setModified();
        return this;
    }

    /**
     * Releases underlying pdf object and other pdf entities used by wrapper.
     * This method should be called instead of direct call to {@link PdfObject#release()} if the wrapper is used.
     */
    public void release() {
        unsetForbidRelease();
        getPdfObject().release();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isWrappedObjectMustBeIndirect() {
        return true;
    }

    /**
     * Gets the {@link PdfDocument} that owns that form field.
     *
     * @return the {@link PdfDocument} that owns that form field.
     */
    protected PdfDocument getDocument() {
        return getPdfObject().getIndirectReference().getDocument();
    }


    /**
     * Sets the text color and regenerates appearance stream.
     *
     * @param color the new value for the Color.
     * @return the edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setColor(Color color) {
        this.color = color;
        regenerateField();
        return this;
    }

    /**
     * Basic setter for the <code>font</code> property. Regenerates the field
     * appearance after setting the new value.
     * Note that the font will be added to the document so ensure that the font is embedded
     * if it's a pdf/a document.
     *
     * @param font The new font to be set.
     * @return The edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setFont(PdfFont font) {
        updateFontAndFontSize(font, this.fontSize);
        regenerateField();
        return this;
    }

    /**
     * Basic setter for the <code>fontSize</code> property. Regenerates the
     * field appearance after setting the new value.
     *
     * @param fontSize The new font size to be set.
     * @return The edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setFontSize(float fontSize) {
        updateFontAndFontSize(this.font, fontSize);
        regenerateField();
        return this;
    }

    /**
     * Basic setter for the <code>fontSize</code> property. Regenerates the
     * field appearance after setting the new value.
     *
     * @param fontSize The new font size to be set.
     * @return The edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setFontSize(int fontSize) {
        setFontSize((float) fontSize);
        return this;
    }

    /**
     * Sets zero font size which will be interpreted as auto-size according to ISO 32000-1, 12.7.3.3.
     *
     * @return the edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setFontSizeAutoScale() {
        this.fontSize = 0;
        regenerateField();

        return this;
    }

    /**
     * Combined setter for the <code>font</code> and <code>fontSize</code>
     * properties. Regenerates the field appearance after setting the new value.
     *
     * @param font     The new font to be set.
     * @param fontSize The new font size to be set.
     * @return The edited {@link AbstractPdfFormField}.
     */
    public AbstractPdfFormField setFontAndSize(PdfFont font, float fontSize) {
        updateFontAndFontSize(font, fontSize);
        regenerateField();
        return this;
    }

    void updateFontAndFontSize(PdfFont font, float fontSize) {
        this.font = font;
        this.fontSize = fontSize;
    }

    void retrieveStyles() {
        PdfString defaultAppearance = getDefaultAppearance();
        if (defaultAppearance != null) {
            Object[] fontData = splitDAelements(defaultAppearance.getValue());
            if (fontData[DA_SIZE] != null && fontData[DA_FONT] != null) {
                color = (Color) fontData[DA_COLOR];
                fontSize = (float) fontData[DA_SIZE];
                font = resolveFontName((String) fontData[DA_FONT]);
            }
        }
    }

    PdfObject getAcroFormObject(PdfName key, int type) {
        PdfObject acroFormObject = null;
        PdfDictionary acroFormDictionary = getDocument().getCatalog().getPdfObject().getAsDictionary(PdfName.AcroForm);
        if (acroFormDictionary != null) {
            acroFormObject = acroFormDictionary.get(key);
        }
        return (acroFormObject != null && acroFormObject.getType() == type) ? acroFormObject : null;
    }

    private static Object[] splitDAelements(String da) {
        PdfTokenizer tk = new PdfTokenizer(new RandomAccessFileOrArray(new RandomAccessSourceFactory().createSource(
                PdfEncodings.convertToBytes(da, null))));
        List<String> stack = new ArrayList<>();
        Object[] ret = new Object[3];
        try {
            while (tk.nextToken()) {
                if (tk.getTokenType() == PdfTokenizer.TokenType.Comment) {
                    continue;
                }
                if (tk.getTokenType() == PdfTokenizer.TokenType.Other) {
                    switch (tk.getStringValue()) {
                        case "Tf":
                            if (stack.size() >= 2) {
                                ret[DA_FONT] = stack.get(stack.size() - 2);
                                ret[DA_SIZE] = new Float(stack.get(stack.size() - 1));
                            }
                            break;
                        case "g":
                            if (stack.size() >= 1) {
                                float gray = new Float(stack.get(stack.size() - 1));
                                if (gray != 0) {
                                    ret[DA_COLOR] = new DeviceGray(gray);
                                }
                            }
                            break;
                        case "rg":
                            if (stack.size() >= 3) {
                                float red = new Float(stack.get(stack.size() - 3));
                                float green = new Float(stack.get(stack.size() - 2));
                                float blue = new Float(stack.get(stack.size() - 1));
                                ret[DA_COLOR] = new DeviceRgb(red, green, blue);
                            }
                            break;
                        case "k":
                            if (stack.size() >= 4) {
                                float cyan = new Float(stack.get(stack.size() - 4));
                                float magenta = new Float(stack.get(stack.size() - 3));
                                float yellow = new Float(stack.get(stack.size() - 2));
                                float black = new Float(stack.get(stack.size() - 1));
                                ret[DA_COLOR] = new DeviceCmyk(cyan, magenta, yellow, black);
                            }
                            break;
                        default:
                            stack.clear();
                            break;
                    }
                } else {
                    stack.add(tk.getStringValue());
                }
            }
        } catch (Exception ignored) {

        }
        return ret;
    }

    private PdfFont resolveFontName(String fontName) {
        PdfDictionary defaultResources = (PdfDictionary) getAcroFormObject(PdfName.DR, PdfObject.DICTIONARY);
        PdfDictionary defaultFontDic = defaultResources != null ? defaultResources.getAsDictionary(PdfName.Font) : null;
        if (fontName != null && defaultFontDic != null) {
            PdfDictionary daFontDict = defaultFontDic.getAsDictionary(new PdfName(fontName));
            if (daFontDict != null) {
                return getDocument().getFont(daFontDict);
            }
        }
        return null;
    }
}