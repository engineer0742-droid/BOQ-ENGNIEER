package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data Exporter utility for QOB (Quantity of Building).
 * Handles production-ready exports of engineering quantity assessments:
 * 1. Excel RTL Spreadsheet (.xls via XML Spreadsheet 2003)
 * 2. Raw LaTeX/XeLaTeX Arabic Source Document (.tex)
 * 3. High-Quality, unalterable PDF document generation via standard Android HTML layout printing
 */
object DataExporter {

    /**
     * Parses amount labels (e.g., "120 م٢", "12 طن", "كامل التأسيس والصحي")
     * into a clean Pair(Quantity, Unit) for international BOQ formats.
     */
    fun parseAmountLabel(label: String): Pair<Double, String> {
        val clean = label.trim()
        if (clean.isEmpty()) return Pair(1.0, "مقطوع")
        
        // Convert Arabic digits to English digits
        val englishDigitsLabel = clean.map { char ->
            when (char) {
                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'
                else -> char
            }
        }.joinToString("")
        
        // Regular expression to find a leading decimal/integer number
        val regex = Regex("""^([0-9]+\.?[0-9]*)\s*(.*)$""")
        val match = regex.find(englishDigitsLabel)
        if (match != null) {
            val qtyStr = match.groupValues[1]
            val unitStr = match.groupValues[2].trim()
            val qty = qtyStr.toDoubleOrNull() ?: 1.0
            val unit = if (unitStr.isNotEmpty()) unitStr else "وحدة"
            return Pair(qty, unit)
        }
        
        return Pair(1.0, "مقطوع")
    }

    data class ExportMetadata(
        val projectName: String,
        val location: String,
        val date: String,
        val engineer: String,
        val totalEstimatedCost: Double,
        val concreteVolumeM3: Double,
        val totalAreaM2: Double
    )

    data class ExportItem(
        val name: String,
        val description: String,
        val quantity: String,
        val approximateCost: Double,
        val notes: String = ""
    )

    /**
     * Generates a fully formatted Microsoft Excel XML Spreadsheet (RTL) containing 
     * metadata and quantity records, saving it to cache and sharing it natively.
     */
    fun exportToExcel(
        context: Context,
        metadata: ExportMetadata,
        items: List<ExportItem>
    ) {
        exportToCsvExcel(context, metadata, items)
        return
        val xlsContent = StringBuilder().apply {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<?mso-application progid=\"Excel.Sheet\"?>\n")
            append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")
            
            // Document Styles Settings
            append(" <Styles>\n")
            append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
            append("   <Alignment ss:Vertical=\"Center\"/>\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Color=\"#000000\"/>\n")
            append("  </Style>\n")
            
            // Primary Header Style (Deep Civil Blue)
            append("  <Style ss:ID=\"Header\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
            append("   <Interior ss:Color=\"#0B3C5D\" ss:Pattern=\"Solid\"/>\n")
            append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n")
            append("   <Borders>\n")
            append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#000000\"/>\n")
            append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#000000\"/>\n")
            append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#000000\"/>\n")
            append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#000000\"/>\n")
            append("   </Borders>\n")
            append("  </Style>\n")

            // Title block font style
            append("  <Style ss:ID=\"AppTitle\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#0B3C5D\"/>\n")
            append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            append("  </Style>\n")

            // Meta labels font style
            append("  <Style ss:ID=\"MetaLabel\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#328CC1\"/>\n")
            append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            append("  </Style>\n")

            // Meta values style
            append("  <Style ss:ID=\"MetaValue\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#1D2731\"/>\n")
            append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            append("  </Style>\n")

            // Standard Data cell
            append("  <Style ss:ID=\"DataCell\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"11\"/>\n")
            append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n")
            append("   <Borders>\n")
            append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("   </Borders>\n")
            append("  </Style>\n")

            // Cost cell highlights
            append("  <Style ss:ID=\"CostCell\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#B30000\"/>\n")
            append("   <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>\n")
            append("   <Borders>\n")
            append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            append("   </Borders>\n")
            append("  </Style>\n")

            // Totals Row style
            append("  <Style ss:ID=\"TotalRowStyle\">\n")
            append("   <Font ss:FontName=\"Arial\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#0B3C5D\"/>\n")
            append("   <Interior ss:Color=\"#ECF0F1\" ss:Pattern=\"Solid\"/>\n")
            append("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            append("   <Borders>\n")
            append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\" ss:Color=\"#0B3C5D\"/>\n")
            append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\" ss:Color=\"#0B3C5D\"/>\n")
            append("   </Borders>\n")
            append("  </Style>\n")
            append(" </Styles>\n")

            // Sheet Declaration
            append(" <Worksheet ss:Name=\"كشف كميات ومواد البناء\">\n")
            
            // FORCE RIGHT-TO-LEFT LAYOUT FOR PROFESSIONAL ARABIC WORKBOOK
            append("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">\n")
            append("   <DisplayRightToLeft/>\n")
            append("  </WorksheetOptions>\n")

            append("  <Table ss:ExpandedColumnCount=\"7\" ss:DefaultRowHeight=\"22\">\n")
            // Define Column Widths
            append("   <Column ss:Width=\"80\"/>\n")  // Reference Code
            append("   <Column ss:Width=\"260\"/>\n") // Civil Element & Specs Description
            append("   <Column ss:Width=\"90\"/>\n")  // Unit
            append("   <Column ss:Width=\"90\"/>\n")  // Quantity
            append("   <Column ss:Width=\"130\"/>\n") // Unit Rate
            append("   <Column ss:Width=\"140\"/>\n") // Total Amount
            append("   <Column ss:Width=\"180\"/>\n") // Site Notes

            // Title Block
            append("   <Row ss:Height=\"30\">\n")
            append("    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"AppTitle\"><Data ss:Type=\"String\">جدول كميات ومواد البناء التفصيلي (BOQ) - طبقاً لقائمة الكلف العراقية</Data></Cell>\n")
            append("   </Row>\n")
            append("   <Row ss:Height=\"5\"/>\n") // Empty Spacer Row

            // Metadata block
            append("   <Row>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">المشروع الإجمالي:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">${metadata.projectName}</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">الموقع ومحافظة العمل:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">${metadata.location}</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("   </Row>\n")

            append("   <Row>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">المهندس المسؤول:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">${metadata.engineer}</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">تاريخ تنظيم الحساب:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">${metadata.date}</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("   </Row>\n")

            append("   <Row>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">المساحة المقدرة:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">${String.format("%.1f", metadata.totalAreaM2)} م٢</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">موثوقية الكلف:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"><Data ss:Type=\"String\">مطابقة لأسعار السوق العراقي الحالي</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("    <Cell ss:StyleID=\"MetaValue\"/>\n")
            append("   </Row>\n")
            append("   <Row ss:Height=\"15\"/>\n") // Empty Spacer

            // Table Headers for International BOQ
            append("   <Row ss:Height=\"24\">\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">رمز البند</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">العنصر والمواصفة الفنية للعمل</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">الوحدة</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">الكمية</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">سعر المفرد (د.ع)</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">المبلغ الإجمالي (د.ع)</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">ملاحظات وتوصيات تنفيذية</Data></Cell>\n")
            append("   </Row>\n")

            // Table Data Rows
            var index = 1
            for (item in items) {
                val parsed = parseAmountLabel(item.quantity)
                val qtyStr = String.format(Locale.US, "%.1f", parsed.first)
                val unitStr = parsed.second
                val rate = if (parsed.first > 0) item.approximateCost / parsed.first else item.approximateCost
                val itemCode = "BOQ-1.${String.format("%02d", index)}"
                index++

                append("   <Row ss:Height=\"26\">\n")
                append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$itemCode</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(item.name)} - ${escapeXml(item.description)}</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(unitStr)}</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$qtyStr</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"CostCell\"><Data ss:Type=\"String\">${formatCostIraqi(rate)}</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"CostCell\"><Data ss:Type=\"String\">${formatCostIraqi(item.approximateCost)}</Data></Cell>\n")
                append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(item.notes.ifEmpty { "تخضع للتعديل الموقعي بمعدل هدر 5%-8%" })}</Data></Cell>\n")
                append("   </Row>\n")
            }

            // Total Block Row
            append("   <Row ss:Height=\"24\">\n")
            append("    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"TotalRowStyle\"><Data ss:Type=\"String\">المجموع الكلي التقديري للـ BOQ والمواد الإنشائية:</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"TotalRowStyle\"><Data ss:Type=\"String\">${formatCostIraqi(metadata.totalEstimatedCost)} د.ع</Data></Cell>\n")
            append("    <Cell ss:StyleID=\"TotalRowStyle\"><Data ss:Type=\"String\"/></Cell>\n")
            append("   </Row>\n")

            // Engineering Equation Block inside Sheet for validation
            append("   <Row ss:Height=\"15\"/>\n")
            append("   <Row>\n")
            append("    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">📐 المعادلات الهندسية العالمية والمحلية المعتمدة لحساب الخرسانة والمواد:</Data></Cell>\n")
            append("   </Row>\n")
            append("   <Row>\n")
            append("    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">1. معامل حجم الخرسانة الجافة (Dry Volume Factor):  V_dry = 1.54 * V_wet</Data></Cell>\n")
            append("   </Row>\n")
            append("   <Row>\n")
            append("    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">2. تخمين كمية الإسمنت بالطن:  Cement (Ton) = (1/7) * V_dry * 1400 / 1000</Data></Cell>\n")
            append("   </Row>\n")
            append("   <Row>\n")
            append("    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">3. حجم الرمل والركام بالمكعب (Sand &amp; Gravel):  Sand = (2/7) * V_dry  |  Gravel = (4/7) * V_dry</Data></Cell>\n")
            append("   </Row>\n")

            append("  </Table>\n")
            append(" </Worksheet>\n")
            append("</Workbook>\n")
        }.toString()

        try {
            val cacheFile = File(context.cacheDir, "BOQ_International_Spreadsheet_${System.currentTimeMillis()}.xls")
            FileOutputStream(cacheFile).use { fos ->
                fos.write(xlsContent.toByteArray(Charsets.UTF_8))
            }
            shareFile(context, cacheFile, "application/vnd.ms-excel", "مشاركة مستند Excel BOQ الحسابي")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل تصدير كشف الإكسيل: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Generates a structural and highly professional LaTeX/XeLaTeX document string (.tex format)
     * containing full Arabic polyglossia typesetting configurations and native LaTeX equations.
     */
    fun exportToLaTeX(
        context: Context,
        metadata: ExportMetadata,
        items: List<ExportItem>
    ) {
        val texContent = StringBuilder().apply {
            append("% !EXProgram = xelatex\n")
            append("\\documentclass[11pt,a4paper]{article}\n")
            append("\\usepackage{geometry}\n")
            append("\\geometry{a4paper, margin=0.75in}\n\n")
            
            append("% إعدادات اللغة العربية والترميز الفصيح وحزم الخطوط\n")
            append("\\usepackage{fontspec}\n")
            append("\\usepackage{polyglossia}\n")
            append("\\setmainlanguage{arabic}\n")
            append("\\setotherlanguage{english}\n\n")
            
            append("% تعيين الخطوط العربية الافتراضية المتطابقة مع الأنظمة\n")
            append("\\newfontfamily\\arabicfont[Script=Arabic]{Arial}\n")
            append("\\newfontfamily\\arabicfontsf[Script=Arabic]{Arial}\n\n")
            
            append("% حزم الرياضيات وتصاميم الجداول الفنية الملائمة للمهندس\n")
            append("\\usepackage{amsmath}\n")
            append("\\usepackage{unicode-math}\n")
            append("\\usepackage{booktabs}\n")
            append("\\usepackage{color}\n")
            append("\\usepackage{colortbl}\n")
            append("\\usepackage{hyperref}\n\n")

            append("\\definecolor{deepblue}{rgb}{0.04, 0.23, 0.36}\n")
            append("\\definecolor{lightgray}{rgb}{0.95, 0.95, 0.95}\n\n")

            append("\\title{\\textbf{\\textcolor{deepblue}{تقرير حساب كتل ومواد البناء والكميات الشامل (BOQ)}}} \n")
            append("\\author{تطبيق QOB الذكي لتخمين كلف البناء العراقي} \n")
            append("\\date{\\arabicfont{${metadata.date}}} \n\n")

            append("\\begin{document}\n")
            append("\\maketitle\n\n")

            append("\\section*{المعلومات والبيانات الأساسية للموقع}\n")
            append("\\begin{tabular}{r l}\n")
            append("  \\textbf{اسم الكشف والمشروع:} & \\arabicfont{${metadata.projectName}} \\\\\n")
            append("  \\textbf{محافظة وموقع العمل:} & \\arabicfont{${metadata.location}} \\\\\n")
            append("  \\textbf{المهندس المدني المشرف:} & \\arabicfont{${metadata.engineer}} \\\\\n")
            append("  \\textbf{المساحة التخمينية للمخطط:} & \\arabicfont{${String.format("%.1f", metadata.totalAreaM2)} م٢} \\\\\n")
            append("  \\textbf{الكلفة الإجمالية المقدرة:} & \\arabicfont{${formatCostIraqi(metadata.totalEstimatedCost)} دينار عراقي} \\\\\n")
            append("\\end{tabular}\n\n")

            append("\\section*{📘 المعادلات الهندسية المستعملة في برنامج QOB}\n")
            append("يتم تخمين كميات وحجوم المواد للمنشأ الخرساني الكلي عبر الصيغ الإنشائية المعتمدة محلياً:\n")
            append("\\begin{equation*}\n")
            append("  V_{dry} = 1.54 \\times V_{wet}\n")
            append("\\end{equation*}\n")
            append("ومن ثم استخراج الكتل والركام وتخمينها:\n")
            append("\\begin{equation*}\n")
            append("  C = \\frac{1}{7} \\times V_{dry} \\times 1400 \\quad \\text{(الوزن بالطن للإسمنت)}\n")
            append("\\end{equation*}\n")
            append("\\begin{equation*}\n")
            append("  R = \\frac{2}{7} \\times V_{dry} \\quad , \\quad G = \\frac{4}{7} \\times V_{dry} \\quad \\text{(حجم الرمل والحصى م٣)}\n")
            append("\\end{equation*}\n\n")

            append("\\section*{جدول الكميات التفصيلي المسعر (Bill of Quantities)}\n")
            append("\\begin{center}\n")
            append("\\rowcolors{1}{white}{lightgray}\n")
            append("\\begin{tabular}{p{4.5cm} p{2cm} p{3.5cm} p{5.5cm}}\n")
            append("  \\toprule\n")
            append("  \\rowcolor{deepblue} \\textcolor{white}{\\textbf{العنصر والمادة}} & \\textcolor{white}{\\textbf{الكمية}} & \\textcolor{white}{\\textbf{الكلفة المقدرة}} & \\textcolor{white}{\\textbf{ملاحظات تنفيذية}} \\\\\n")
            append("  \\midrule\n")
            
            for (item in items) {
                val safeName = escapeLatex(item.name)
                val safeQty = escapeLatex(item.quantity)
                val safeCost = formatCostIraqi(item.approximateCost) + " د.ع"
                val safeDesc = escapeLatex(item.description)
                append("  \\arabicfont{$safeName} & \\arabicfont{$safeQty} & \\arabicfont{$safeCost} & \\arabicfont{$safeDesc} \\\\\n")
            }
            
            append("  \\bottomrule\n")
            append("\\end{tabular}\n")
            append("\\end{center}\n\n")

            append("\\section*{توصيات موقعية وتنبيهات حقلية (ملاحظات من تطبيق QOB)}\n")
            append("\\begin{itemize}\n")
            append("  \\item يعتمد هذا الحساب على أسعار المواد الأولية الحية المطابقة لوزارة التخطيط العراقية ونقابة المهندسين.\n")
            append("  \\item يجب على المنفذ التأكد من مطابقة أوزان حديد التسليح ونسبة الخلط الخرساني (1:2:4) للجريدة الإرشادية.\n")
            append("  \\item تم احتساب نسبة هدر موحدة للمواد الأساسية (الإسمنت والرمل والطابوق) تتراوح بين 5\\% إلى 8\\%.\n")
            append("\\end{itemize}\n\n")

            append("\\end{document}\n")
        }.toString()

        try {
            val cacheFile = File(context.cacheDir, "QOB_XeLaTeX_Report_${System.currentTimeMillis()}.tex")
            FileOutputStream(cacheFile).use { fos ->
                fos.write(texContent.toByteArray(Charsets.UTF_8))
            }
            shareFile(context, cacheFile, "text/plain", "مشاركة مستند وتصميم XeLaTeX المصدر")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل تصدير مستند XeLaTeX: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Active WebView reference holder to prevent garbage collection during document print flow
    @Volatile
    private var activeWebView: WebView? = null

    /**
     * Generates a fully formatted, beautifully stylized PDF document natively on Android
     * using the system's HTML/WebView printing system. This guarantees offline excellence
     * and a flawless print-to-PDF output completely localized to Arabic and RTL formatting.
     */
    fun exportToPdf(
        context: Context,
        metadata: ExportMetadata,
        items: List<ExportItem>
    ) {
        val htmlContent = StringBuilder().apply {
            append("<!DOCTYPE html>\n")
            append("<html dir=\"rtl\" lang=\"ar\">\n")
            append("<head>\n")
            append("<meta charset=\"UTF-8\">\n")
            append("<title>كشف كميات ومواد البناء الشامل - تطبيق QOB</title>\n")
            append("<style>\n")
            append("  body { font-family: 'Arial', sans-serif; margin: 30px; color: #2c3e50; background-color: #ffffff; }\n")
            append("  .header-container { border-bottom: 3px double #0d3c5c; padding-bottom: 12px; margin-bottom: 24px; }\n")
            append("  .title { font-size: 24px; font-weight: bold; color: #0d3c5c; text-align: center; margin: 0; }\n")
            append("  .subtitle { font-size: 14px; text-align: center; color: #7f8c8d; margin-top: 6px; }\n")
            append("  .metadata-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; padding: 12px; background-color: #f8f9fa; border-radius: 8px; border: 1px solid #e2e8f0; }\n")
            append("  .metadata-item { font-size: 13px; }\n")
            append("  .metadata-item strong { color: #0d3c5c; }\n")
            append("  .equation-box { background-color: #ebf5fb; border-right: 5px solid #2980b9; padding: 14px; margin-bottom: 24px; border-radius: 4px; font-size: 13px; line-height: 1.6; }\n")
            append("  .equation { font-family: 'Courier New', monospace; font-weight: bold; color: #c0392b; text-align: center; margin: 8px 0; font-size: 14px; }\n")
            append("  table { width: 100%; border-collapse: collapse; margin-bottom: 24px; font-size: 12px; }\n")
            append("  th { background-color: #0d3c5c; color: #ffffff; padding: 10px; text-align: right; font-weight: bold; border: 1px solid #bdc3c7; }\n")
            append("  td { padding: 8px 10px; border: 1px solid #bdc3c7; text-align: right; }\n")
            append("  tr:nth-child(even) { background-color: #f2f4f4; }\n")
            append("  .cost-col { font-weight: bold; color: #c0392b; }\n")
            append("  .total-row { font-size: 14px; font-weight: bold; background-color: #d5dbdb !important; color: #0d3c5c; }\n")
            append("  .footer-notes { font-size: 11px; color: #7f8c8d; line-height: 1.5; margin-top: 30px; border-top: 1px dashed #bdc3c7; padding-top: 10px; }\n")
            append("  @media print {\n")
            append("    body { margin: 15px; }\n")
            append("    .no-print { display: none; }\n")
            append("  }\n")
            append("</style>\n")
            append("</head>\n")
            append("<body>\n")

            // Title block
            append("<div class=\"header-container\">\n")
            append("  <div class=\"title\">المخطط التكميلي وكشف المواد الهندسي الشامل</div>\n")
            append("  <div class=\"subtitle\">صادر من تطبيق QOB الذكي لتقدير كميات وصيانة المباني السكنية</div>\n")
            append("</div>\n")

            // Metadata block
            append("<div class=\"metadata-grid\">\n")
            append("  <div class=\"metadata-item\"><strong>اسم المشروع والمسودة:</strong> ${metadata.projectName}</div>\n")
            append("  <div class=\"metadata-item\"><strong>الموقع والمنطقة الجغرافية:</strong> ${metadata.location}</div>\n")
            append("  <div class=\"metadata-item\"><strong>المهندس المدني المشرف:</strong> ${metadata.engineer}</div>\n")
            append("  <div class=\"metadata-item\"><strong>تاريخ تحرير الكشف المالي:</strong> ${metadata.date}</div>\n")
            append("  <div class=\"metadata-item\"><strong>المساحة الكلية الإجمالية:</strong> ${String.format("%.1f", metadata.totalAreaM2)} متر مربع</div>\n")
            append("  <div class=\"metadata-item\"><strong>مجموع المكونات المقدرة:</strong> حجم الصب الإنشائي مبرمج بالكامل</div>\n")
            append("</div>\n")

            // Engineering Equation Box
            append("<div class=\"equation-box\">\n")
            append("  <strong>📐 المعادلات الهندسية لتخمين الخرسانة والمواد (طبقاً لنقابة المهندسين العراقيين):</strong>\n")
            append("  <div class=\"equation\">V_dry = 1.5 dry_factor * V_wet (V_dry = 1.54 &times; V_wet)</div>\n")
            append("  معادلة حساب الإسمنت بالطن للبناء والصب:\n")
            append("<div class=\"equation\">C = (1/7) &times; V_dry &times; 1400 / 1000</div>\n")
            append("  حساب الرمل والحصى والركام المطلوب بموجب حجم صب الخرسانة الرطب والجاف:\n")
            append("  <div class=\"equation\">Sand = 2/7 &times; V_dry &nbsp;&nbsp;|&nbsp;&nbsp; Gravel = 4/7 &times; V_dry</div>\n")
            append("</div>\n")

            // Table of items in International BOQ Standard Layout
            append("<table>\n")
            append("  <thead>\n")
            append("    <tr>\n")
            append("      <th style=\"width: 10%; text-align: center;\">رمز البند</th>\n")
            append("      <th style=\"width: 40%; text-align: right;\">العنصر والمواصفة الفنية للعمل والمنشأ</th>\n")
            append("      <th style=\"width: 10%; text-align: center;\">الوحدة</th>\n")
            append("      <th style=\"width: 10%; text-align: center;\">الكمية</th>\n")
            append("      <th style=\"width: 15%; text-align: left;\">سعر المفرد (د.ع)</th>\n")
            append("      <th style=\"width: 15%; text-align: left;\">المبلغ الإجمالي (د.ع)</th>\n")
            append("    </tr>\n")
            append("  </thead>\n")
            append("  <tbody>\n")

            var itemIdx = 1
            for (item in items) {
                val parsed = parseAmountLabel(item.quantity)
                val qtyStr = String.format(Locale.US, "%.1f", parsed.first)
                val unitStr = parsed.second
                val rate = if (parsed.first > 0) item.approximateCost / parsed.first else item.approximateCost
                val itemCode = "BOQ-1.${String.format("%02d", itemIdx)}"
                itemIdx++

                append("    <tr>\n")
                append("      <td style=\"text-align: center; font-weight: bold; color: #7f8c8d;\">$itemCode</td>\n")
                append("      <td><strong style=\"color: #0d3c5c;\">${item.name}</strong><br><small style=\"color: #555;\">${item.description}</small></td>\n")
                append("      <td style=\"text-align: center; font-weight: bold;\">$unitStr</td>\n")
                append("      <td style=\"text-align: center;\">$qtyStr</td>\n")
                append("      <td class=\"cost-col\" style=\"text-align: left; font-weight: normal;\">${formatCostIraqi(rate)}</td>\n")
                append("      <td class=\"cost-col\" style=\"text-align: left; background-color: #fcf3f2;\">${formatCostIraqi(item.approximateCost)}</td>\n")
                append("    </tr>\n")
            }

            // Total row
            append("    <tr class=\"total-row\">\n")
            append("      <td colspan=\"5\" style=\"text-align: left; padding: 12px; font-weight: bold; font-size: 13px;\">المجموع الكلي المقدر لجدول الكميات والأسعار (BOQ):</td>\n")
            append("      <td style=\"color: #c0392b; text-align: left; font-size: 14px; border-top: 2px double #c0392b; padding: 12px; font-weight: 800; background-color: #f9ebea;\">${formatCostIraqi(metadata.totalEstimatedCost)} د.ع</td>\n")
            append("    </tr>\n")
            append("  </tbody>\n")
            append("</table>\n")

            // Footer
            append("<div class=\"footer-notes\">\n")
            append("  <strong>ملاحظة هامة للمنفذ والمشرف:</strong><br>\n")
            append("  هذا التقدير تم توليده تلقائياً بأحدث الحسابات الرياضية للمنشآت الإنشائية المعتمدة ويهدف لتسهيل اتخاذ القرار وتجنب الخسائر والهدر أثناء توريد الخامات والمواد للمقاول. الأسعار حركية ويمكن تحديثها عبر واجهة إعدادات التعديل ببرنامج QOB.<br>\n")
            append("  تطبيق كميات البناء والترميم العراقي © 2026 QOB (Quantity of Building)\n")
            append("</div>\n")

            append("</body>\n")
            append("</html>\n")
        }.toString()

        try {
            // Runs standard UI thread task to prepare WebView, trigger system build PDF adaptors
            val webView = WebView(context)
            activeWebView = webView
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val printAdapter = webView.createPrintDocumentAdapter("كشف_كميات_QOB_الرسمي")
                        val jobName = "QOB_Estimate_PDF_Report_${System.currentTimeMillis()}"
                        
                        printManager.print(
                            jobName,
                            printAdapter,
                            PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "فشل في تشغيل الطابعة: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(context, "بوابة WebView غير مدعومة على هذا الجهاز الإفتراضي: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.boqpro.wkxmpz.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, chooserTitle)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل طلب التصدير الخارجي: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatCostIraqi(cost: Double): String {
        return try {
            val formatter = java.text.NumberFormat.getNumberInstance(Locale.US)
            formatter.format(cost.toLong())
        } catch (e: Exception) {
            String.format("%,.0f", cost)
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeLatex(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde ")
            .replace("^", "\\textasciicircum ")
    }

    fun exportToCsvExcel(
        context: Context,
        metadata: ExportMetadata,
        items: List<ExportItem>
    ) {
        val csvBuilder = java.lang.StringBuilder().apply {
            // First line: sep=, to force Excel to use comma separator in all locales
            append("sep=,\n")
            
            // Header Title Block
            append(escapeCsv("جدول كميات ومواد البناء التفصيلي (BOQ) - طبقاً لقائمة الكلف العراقية")).append("\n")
            append("\n")
            
            // Metadata Rows
            append("${escapeCsv("اسم المشروع الإجمالي:")},${escapeCsv(cleanArabicExcelText(metadata.projectName))},,${escapeCsv("الموقع ومحافظة العمل:")},${escapeCsv(cleanArabicExcelText(metadata.location))}\n")
            append("${escapeCsv("المهندس المسؤول:")},${escapeCsv(cleanArabicExcelText(metadata.engineer))},,${escapeCsv("تاريخ تنظيم الحساب:")},${escapeCsv(cleanArabicExcelText(metadata.date))}\n")
            append("${escapeCsv("المساحة المقدرة:")},${escapeCsv(String.format(Locale.US, "%.1f", metadata.totalAreaM2) + " م٢")},,${escapeCsv("موثوقية الكلف:")},${escapeCsv("مستند مالي مطابق لأسعار السوق العراقي الحالي")}\n")
            append("\n")
            
            // Table Headers
            append("${escapeCsv("رمز البند")},${escapeCsv("العنصر والمواصفة الفنية للعمل")},${escapeCsv("الوحدة")},${escapeCsv("الكمية")},${escapeCsv("سعر المفرد (د.ع)")},${escapeCsv("المبلغ الإجمالي (د.ع)")},${escapeCsv("ملاحظات وتوصيات تنفيذية")}\n")
            
            // Table Data Rows
            var index = 1
            for (item in items) {
                val parsed = parseAmountLabel(item.quantity)
                val qtyStr = String.format(Locale.US, "%.1f", parsed.first)
                val unitStr = parsed.second
                val rate = if (parsed.first > 0) item.approximateCost / parsed.first else item.approximateCost
                val itemCode = "بند 1.${String.format(Locale.US, "%02d", index)}"
                index++
                
                val fullDescription = "${cleanArabicExcelText(item.name)} - ${cleanArabicExcelText(item.description)}"
                val defaultNotes = cleanArabicExcelText(item.notes.ifEmpty { "تخضع للتعديل الموقعي بمعدل هدر 5%-8%" })
                
                append("${escapeCsv(itemCode)},")
                append("${escapeCsv(fullDescription)},")
                append("${escapeCsv(cleanArabicExcelText(unitStr))},")
                append("${escapeCsv(qtyStr)},")
                append("${escapeCsv(formatCostIraqi(rate))},")
                append("${escapeCsv(formatCostIraqi(item.approximateCost))},")
                append("${escapeCsv(defaultNotes)}\n")
            }
            
            // Total Row
            append("${escapeCsv("المجموع الكلي التقديري للـ BOQ والمواد الإنشائية:")},,,,,${escapeCsv(formatCostIraqi(metadata.totalEstimatedCost) + " د.ع")},\n")
            append("\n")
            
            // Computational/Engineering Equations (Translated to Arabic and numbers only)
            append("${escapeCsv("📐 المعادلات الهندسية العالمية والمحلية المعتمدة لحساب الخرسانة والمواد:")},,,,,,\n")
            append("${escapeCsv("1. معامل حجم الخرسانة الجافة: الحجم الجاف = 1.54 * الحجم الرطب")},,,,,,\n")
            append("${escapeCsv("2. تخمين كمية الإسمنت بالطن: الإسمنت = (1 / 7) * الحجم الجاف * 1400 / 1000")},,,,,,\n")
            append("${escapeCsv("3. حجم الرمل والركام بالمكعب: الرمل = (2 / 7) * الحجم الجاف | الحصى = (4 / 7) * الحجم الجاف")},,,,,,\n")
        }

        try {
            val cacheFile = File(context.cacheDir, "BOQ_Spreadsheet_${System.currentTimeMillis()}.csv")
            FileOutputStream(cacheFile).use { fos ->
                // Write UTF-8 Byte Order Mark (BOM) to ensure Excel displays Arabic characters beautifully
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                fos.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
            }
            shareFile(context, cacheFile, "text/csv", "مشاركة مستند Excel BOQ الحسابي")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل تصدير كشف الإكسيل: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cleanArabicExcelText(str: String): String {
        // 1. Remove parenthesized blocks containing any Latin/English letters
        var result = str.replace(Regex("\\s*\\([^)]*[a-zA-Z][^)]*\\)"), "")
        // 2. Also remove brace/bracket blocks containing Latin letters
        result = result.replace(Regex("\\s*\\[[^]]*[a-zA-Z][^]]*\\]"), "")
        
        // 3. Keep only Arabic characters, numbers (Western & Arabic-Indic), spaces, and standard pricing punctuation
        val sb = java.lang.StringBuilder()
        for (ch in result) {
            val code = ch.code
            if ((code in 0x0600..0x06FF) || (code in 0x0750..0x077F) || (code in 0x08A0..0x08FF) || 
                (code in 0xFB50..0xFDFF) || (code in 0xFE70..0xFEFF) ||
                (ch in '0'..'9') || (ch in '٠'..'٩') ||
                ch.isWhitespace() || 
                ch == '.' || ch == ',' || ch == '،' || ch == '%' || ch == '٪' || 
                ch == '-' || ch == '+' || ch == '/' || ch == ':' || ch == '*' || ch == '=' || ch == '(' || ch == ')') {
                sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun escapeCsv(str: String): String {
        val clean = str.replace("\n", " ").replace("\r", " ").trim()
        if (clean.contains("\"") || clean.contains(",") || clean.contains(";") || clean.contains("\t")) {
            return "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return "\"" + clean + "\""
    }
}
