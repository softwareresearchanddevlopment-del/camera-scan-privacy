package com.example.data.model

object DocumentTemplates {

    val templates: List<DocTemplate> = listOf(
        DocTemplate(
            id = "template_invoice",
            title = "Professional Invoice",
            category = "Business",
            description = "Clean corporate billing receipt with itemized table & payment details.",
            blocks = listOf(
                DocBlock(type = DocBlockType.TITLE, text = "INVOICE"),
                DocBlock(type = DocBlockType.SUBTITLE, text = "Invoice No: INV-2026-0042 • Date: Nov 12, 2026"),
                DocBlock(type = DocBlockType.HEADING_2, text = "Billed To:"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "Acme Global Enterprises\nAttn: Accounts Payable\n100 Enterprise Way, Suite 400"),
                DocBlock(
                    type = DocBlockType.TABLE,
                    tableData = listOf(
                        listOf("Description", "Hours / Qty", "Rate", "Amount"),
                        listOf("Document System Engineering", "30 hrs", "$120/hr", "$3,600.00"),
                        listOf("Mobile UI Architecture", "15 hrs", "$110/hr", "$1,650.00"),
                        listOf("Security & Offline Auditing", "8 hrs", "$125/hr", "$1,000.00"),
                        listOf("Total Due:", "", "", "$6,250.00")
                    )
                ),
                DocBlock(type = DocBlockType.QUOTE, text = "Payment terms: Net 30 days. Please wire payment to bank account on file. Thank you for your business!"),
                DocBlock(type = DocBlockType.CAPTION, text = "Authorized Signature: Camera Scan Certified")
            )
        ),
        DocTemplate(
            id = "template_proposal",
            title = "Executive Business Proposal",
            category = "Business",
            description = "High-impact proposal with problem statement, roadmap and milestones.",
            blocks = listOf(
                DocBlock(type = DocBlockType.TITLE, text = "Executive Project Proposal"),
                DocBlock(type = DocBlockType.SUBTITLE, text = "Prepared for: Executive Leadership Team"),
                DocBlock(type = DocBlockType.HEADING_1, text = "1. Executive Summary"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "This proposal outlines the strategy to modernize enterprise document infrastructure, transitioning from fragmented desktop software to a unified mobile document studio."),
                DocBlock(type = DocBlockType.HEADING_1, text = "2. Key Objectives & Scope"),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• Eliminate dependence on costly third-party cloud conversion APIs."),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• Ensure 100% on-device privacy compliance for confidential client files."),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• Provide multi-page camera scanning, PDF editing, and native Office viewing."),
                DocBlock(type = DocBlockType.HEADING_2, text = "3. Deliverables & Schedule"),
                DocBlock(
                    type = DocBlockType.TABLE,
                    tableData = listOf(
                        listOf("Phase", "Milestone", "Target Date"),
                        listOf("Phase 1", "Core Engine & SAF Integration", "Q1 2027"),
                        listOf("Phase 2", "Advanced Canvas Editor & PPTX", "Q2 2027"),
                        listOf("Phase 3", "Production Launch & Rollout", "Q3 2027")
                    )
                ),
                DocBlock(type = DocBlockType.QUOTE, text = "Recommendation: Approve Phase 1 initiation with immediate effect.")
            )
        ),
        DocTemplate(
            id = "template_resume",
            title = "Modern Professional Resume",
            category = "Resumes",
            description = "Elegant minimalist CV highlighting experience, skills and achievements.",
            blocks = listOf(
                DocBlock(type = DocBlockType.TITLE, text = "ALEXANDER CHEN"),
                DocBlock(type = DocBlockType.SUBTITLE, text = "Lead Android Architect & Systems Engineer • alex@camerascan.app • (555) 234-5678"),
                DocBlock(type = DocBlockType.HEADING_1, text = "Professional Summary"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "Senior software engineer with 8+ years developing high-performance native Android applications, low-level document processing engines, and clean Jetpack Compose user interfaces."),
                DocBlock(type = DocBlockType.HEADING_1, text = "Core Competencies"),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• Kotlin, Jetpack Compose, Coroutines & Flow, Clean MVVM Architecture"),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• Android CameraX, PdfDocument, PdfRenderer, MediaStore & SAF"),
                DocBlock(type = DocBlockType.BULLET_LIST_ITEM, text = "• OpenXML (DOCX, PPTX, XLSX) low-level binary & XML generation"),
                DocBlock(type = DocBlockType.HEADING_1, text = "Work Experience"),
                DocBlock(type = DocBlockType.HEADING_2, text = "Staff Software Engineer | Camera Scan Studios (2023 – Present)"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "Architected offline document engine processing millions of pages without remote cloud roundtrips, reducing latency by 90%."),
                DocBlock(type = DocBlockType.HEADING_2, text = "Senior Android Developer | Apex Technologies (2019 – 2023)"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "Led engineering of enterprise tablet productivity suite featuring drag-and-drop page reorganizers and digital ink canvases.")
            )
        ),
        DocTemplate(
            id = "template_meeting",
            title = "Meeting Minutes & Action Items",
            category = "Reports",
            description = "Structured agenda, attendees, discussion summary, and assigned tasks.",
            blocks = listOf(
                DocBlock(type = DocBlockType.TITLE, text = "Product Alignment Meeting Minutes"),
                DocBlock(type = DocBlockType.SUBTITLE, text = "Date: November 12, 2026 • Time: 10:00 AM – 11:30 AM • Location: Conference Room B"),
                DocBlock(type = DocBlockType.HEADING_2, text = "Attendees:"),
                DocBlock(type = DocBlockType.PARAGRAPH, text = "Sarah Jenkins (PM), David Kim (Tech Lead), Maya Patel (Design), Marcus Vance (QA)"),
                DocBlock(type = DocBlockType.HEADING_1, text = "Agenda Items"),
                DocBlock(type = DocBlockType.NUMBERED_LIST_ITEM, text = "1. Review real device file library implementation progress."),
                DocBlock(type = DocBlockType.NUMBERED_LIST_ITEM, text = "2. Demonstrate CameraX multi-page document scanner."),
                DocBlock(type = DocBlockType.NUMBERED_LIST_ITEM, text = "3. Finalize Image to PPTX presentation generation specs."),
                DocBlock(type = DocBlockType.HEADING_1, text = "Decisions & Action Items"),
                DocBlock(
                    type = DocBlockType.TABLE,
                    tableData = listOf(
                        listOf("Action Item", "Owner", "Status"),
                        listOf("Persist SAF folder tree permissions", "David", "Completed"),
                        listOf("Polish Liquid Glass iOS top bar", "Maya", "In Progress"),
                        listOf("Run Robolectric test suite across all flows", "Marcus", "Scheduled")
                    )
                )
            )
        )
    )
}
