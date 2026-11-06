import os

def main():
    target_path = 'app/src/main/java/com/mtermux/app/TermuxActivity.kt'
    with open(target_path, 'r', encoding='utf-8') as f:
        content = f.read()

    start_marker = "        rebuildUniversalTermPill = rebuildPillLambda@ { animateMorph ->"
    end_marker = "        rebuildUniversalTermPill(false)\n\n        // Initial shared-element morph from top-left Sessions pill"

    start_idx = content.find(start_marker)
    if start_idx == -1:
        raise ValueError("Could not find start_marker!")

    end_idx = content.find(end_marker)
    if end_idx == -1:
        raise ValueError("Could not find end_marker!")

    # Check that start_idx < end_idx
    assert start_idx < end_idx, f"start_idx {start_idx} is not before end_idx {end_idx}"

    replacement_code = '''        rebuildUniversalTermPill = rebuildPillLambda@ { animateMorph ->
            if (activeTab != 1) {
                termDrawerExpanded = false
            }
            val isFileSelectionMode = (activeTab == 1 && selectedSftpPaths.isNotEmpty())
            val isPkgSelectionMode = (activeTab == 2 && selectedPackageNames.isNotEmpty())
            val targetMode = when {
                isFileSelectionMode -> 1
                isPkgSelectionMode -> 2
                activeTab == 1 && termDrawerExpanded -> 3
                else -> 0
            }
            val hasPasteClipboard = (activeTab == 1 && activeSftpServer == null && (fileClipSource != null || batchClipSources.isNotEmpty()))
            val srvNull = (activeSftpServer == null)
            val srcCodeNow = currentSftpSourceCode()

            // Fast in-place counter update when already in File or Package selection mode!
            // Completely eliminates top bar twitching when selecting/deselecting items!
            if (
                (targetMode == 1 || targetMode == 2) &&
                targetMode == currentTermPillMode &&
                trashConfirmExpanded == lastTermPillTrashConfirm &&
                srvNull == lastTermPillSrvNull &&
                termPillCountTv != null
            ) {
                val newCountText = if (targetMode == 1) "${selectedSftpPaths.size}" else "${selectedPackageNames.size}"
                val countView = termPillCountTv!!
                if (countView.text.toString() != newCountText) {
                    countView.text = newCountText
                    animEngine.pulse(countView, 0.82f, 160L)
                }
                return@rebuildPillLambda
            }

            fun makeSegmentPill(
                icon: M3Icon?,
                label: String?,
                leftR: Float,
                rightR: Float,
                fillCol: Int,
                tintCol: Int,
                marginEndDp: Int = 2,
                weightVal: Float = 0f,
                minW: Int = dp(34),
                captureLabelRef: Boolean = false,
                onClick: () -> Unit
            ): LinearLayout {
                return LinearLayout(this@TermuxActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    minimumWidth = minW
                    background = makeConnectedSegmentDrawable(fillCol, leftR, rightR)
                    val hPad = if (label.isNullOrEmpty()) dp(8) else dp(10)
                    setPadding(hPad, 0, hPad, 0)
                    layoutParams = LinearLayout.LayoutParams(
                        if (weightVal > 0f) 0 else ViewGroup.LayoutParams.WRAP_CONTENT,
                        pillInnerH,
                        weightVal
                    ).apply {
                        if (marginEndDp > 0) marginEnd = dp(marginEndDp)
                    }
                    if (icon != null) {
                        addView(
                            MaterialIconView(this@TermuxActivity, icon, tintCol, 17.5f, 2.0f).apply {
                                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                                    if (!label.isNullOrEmpty()) marginEnd = dp(5)
                                }
                            }
                        )
                    }
                    if (!label.isNullOrEmpty()) {
                        val tv = TextView(this@TermuxActivity).apply {
                            text = label
                            setSingleLine(true)
                            setTextColor(tintCol)
                            typeface = Typeface.DEFAULT_BOLD
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        }
                        if (captureLabelRef) {
                            termPillCountTv = tv
                        }
                        addView(tv)
                    }
                    attachSmoothPressAnimation(this, 0.93f)
                    setOnClickListener {
                        triggerHaptic(0.8f)
                        onClick()
                    }
                }
            }

            fun createAccessoryPillView(): LinearLayout {
                return if (hasPasteClipboard) {
                    val countClip = if (batchClipSources.isNotEmpty()) batchClipSources.size else 1
                    makeSegmentPill(
                        icon = M3Icon.COPY,
                        label = tr("Вставить ($countClip)", "Paste ($countClip)"),
                        leftR = pillInnerR,
                        rightR = pillOuterR,
                        fillCol = uiSurfaceHigh,
                        tintCol = uiAccent,
                        marginEndDp = 0
                    ) {
                        val sources = if (batchClipSources.isNotEmpty()) {
                            batchClipSources.map { File(it) }
                        } else {
                            listOfNotNull(fileClipSource)
                        }
                        val isMove = if (batchClipSources.isNotEmpty()) batchClipIsMove else fileClipIsMove
                        Thread {
                            for (srcFile in sources) {
                                val dst = File(localCurrentDir, srcFile.name)
                                var handledDirect = false
                                try {
                                    if (srcFile.exists() && localCurrentDir.canWrite()) {
                                        if (srcFile.isDirectory) srcFile.copyRecursively(dst, overwrite = true)
                                        else srcFile.copyTo(dst, overwrite = true)
                                        if (isMove) srcFile.deleteRecursively()
                                        handledDirect = true
                                    }
                                } catch (_: Throwable) {}
                                if (!handledDirect) {
                                    val op = if (isMove) "mv -f" else "cp -rf"
                                    nativeExecLocalRootCmd("$op -- ${shellEscapeSingle(srcFile.absolutePath)} ${shellEscapeSingle(dst.absolutePath)}")
                                }
                            }
                            runOnUiThread {
                                fileClipSource = null
                                batchClipSources.clear()
                                rebuildUniversalTermPill(true)
                                activeSftpRefreshCallback?.invoke(true, null)
                            }
                        }.start()
                    }
                } else {
                    val srcIcon = when (srcCodeNow) {
                        0 -> M3Icon.FOLDER
                        1 -> M3Icon.STORAGE
                        2 -> M3Icon.ROOT_HASH
                        else -> M3Icon.SERVER
                    }
                    makeSegmentPill(
                        icon = srcIcon,
                        label = "▸",
                        leftR = pillInnerR,
                        rightR = pillOuterR,
                        fillCol = uiSurfaceHigh,
                        tintCol = uiAccent,
                        marginEndDp = 0,
                        minW = 0
                    ) {
                        if (activeTab == 1) {
                            termDrawerExpanded = true
                            rebuildUniversalTermPill(true)
                        }
                    }
                }
            }

            fun buildContentRow(mode: Int): LinearLayout {
                val contentRow = LinearLayout(this@TermuxActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        if (mode != 0) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT,
                        pillInnerH
                    )
                }

                when (mode) {
                    1 -> {
                        val srv = activeSftpServer
                        val activeDirStr = if (srv == null) localCurrentDir.absolutePath else remoteCurrentPath

                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.TERMINAL,
                                label = "${selectedSftpPaths.size}",
                                leftR = pillOuterR,
                                rightR = pillInnerR,
                                fillCol = uiAccentContainer,
                                tintCol = uiOnAccentContainer,
                                weightVal = 1.05f,
                                minW = dp(54),
                                captureLabelRef = true
                            ) {
                                exitToTerminalAnimated()
                            }
                        )
                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.SELECT_ALL,
                                label = null,
                                leftR = pillInnerR,
                                rightR = pillInnerR,
                                fillCol = uiSurfaceHigh,
                                tintCol = COLOR_TEXT_BRIGHT,
                                weightVal = 0.85f
                            ) {
                                trashConfirmExpanded = false
                                if (selectedSftpPaths.size >= lastRenderedEntries.size && lastRenderedEntries.isNotEmpty()) {
                                    selectedSftpPaths.clear()
                                    rebuildUniversalTermPill(true)
                                } else {
                                    for (e in lastRenderedEntries) {
                                        selectedSftpPaths.add(e.path)
                                    }
                                    rebuildUniversalTermPill(false)
                                }
                                activeSftpPopulateRowsCallback?.invoke(null, lastRenderedEntries)
                            }
                        )
                        if (srv == null) {
                            contentRow.addView(
                                makeSegmentPill(
                                    icon = M3Icon.COPY,
                                    label = null,
                                    leftR = pillInnerR,
                                    rightR = pillInnerR,
                                    fillCol = uiSurfaceHigh,
                                    tintCol = COLOR_TEXT_BRIGHT,
                                    weightVal = 0.85f
                                ) {
                                    trashConfirmExpanded = false
                                    batchClipSources.clear()
                                    batchClipSources.addAll(selectedSftpPaths)
                                    batchClipIsMove = false
                                    fileClipSource = null
                                    selectedSftpPaths.clear()
                                    triggerHaptic(0.9f)
                                    toast(tr("Скопировано в буфер", "Copied to clipboard"))
                                    rebuildUniversalTermPill(true)
                                    activeSftpPopulateRowsCallback?.invoke(null, lastRenderedEntries)
                                }
                            )
                            contentRow.addView(
                                makeSegmentPill(
                                    icon = M3Icon.CHMOD,
                                    label = null,
                                    leftR = pillInnerR,
                                    rightR = pillInnerR,
                                    fillCol = uiSurfaceHigh,
                                    tintCol = COLOR_TEXT_BRIGHT,
                                    weightVal = 0.85f
                                ) {
                                    trashConfirmExpanded = false
                                    batchClipSources.clear()
                                    batchClipSources.addAll(selectedSftpPaths)
                                    batchClipIsMove = true
                                    fileClipSource = null
                                    selectedSftpPaths.clear()
                                    triggerHaptic(0.9f)
                                    toast(tr("Вырезано в буфер", "Cut to clipboard"))
                                    rebuildUniversalTermPill(true)
                                    activeSftpPopulateRowsCallback?.invoke(null, lastRenderedEntries)
                                }
                            )
                        }
                        val trashBtn = LinearLayout(this@TermuxActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                            background = makeConnectedSegmentDrawable(
                                COLOR_ROOT_CONTAINER,
                                if (trashConfirmExpanded) pillOuterR else pillInnerR,
                                if (trashConfirmExpanded) pillOuterR else pillInnerR
                            )
                            val hPad = if (trashConfirmExpanded) dp(12) else dp(8)
                            setPadding(hPad, 0, hPad, 0)
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                pillInnerH,
                                if (trashConfirmExpanded) 2.2f else 0.85f
                            ).apply {
                                marginEnd = dp(2)
                            }
                            addView(
                                MaterialIconView(this@TermuxActivity, M3Icon.TRASH, COLOR_ROOT, 17.5f, 2.0f).apply {
                                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                                    if (trashConfirmExpanded) marginEnd = dp(5)
                                }
                            })
                            if (trashConfirmExpanded) {
                                addView(TextView(this@TermuxActivity).apply {
                                    text = tr("Уверены?", "Sure?")
                                    setSingleLine(true)
                                    setTextColor(COLOR_ROOT)
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                })
                            }
                            attachSmoothPressAnimation(this, 0.93f)
                            setOnClickListener {
                                if (!trashConfirmExpanded) {
                                    triggerHaptic(1.1f)
                                    trashConfirmExpanded = true
                                    rebuildUniversalTermPill(true)
                                } else {
                                    triggerHaptic(1.35f)
                                    val targets = selectedSftpPaths.toList()
                                    selectedSftpPaths.clear()
                                    trashConfirmExpanded = false
                                    rebuildUniversalTermPill(true)
                                    Thread {
                                        if (srv != null) {
                                            for (p in targets) {
                                                try {
                                                    sftpDeletePathRecursive(srv, p)
                                                } catch (_: Throwable) {}
                                            }
                                        } else {
                                            for (p in targets) {
                                                val f = File(p)
                                                var ok = false
                                                try {
                                                    if (f.exists()) {
                                                        if (f.isDirectory) f.deleteRecursively() else f.delete()
                                                        ok = !f.exists()
                                                    }
                                                } catch (_: Throwable) {}
                                                if (!ok && f.exists()) {
                                                    nativeExecLocalRootCmd("rm -rf -- ${shellEscapeSingle(p)}")
                                                }
                                            }
                                        }
                                        runOnUiThread {
                                            activeSftpRefreshCallback?.invoke(true, null)
                                        }
                                    }.start()
                                }
                            }
                        }
                        contentRow.addView(trashBtn)

                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.CLOSE,
                                label = null,
                                leftR = pillInnerR,
                                rightR = pillOuterR,
                                fillCol = uiSurfaceHigh,
                                tintCol = COLOR_TEXT_BRIGHT,
                                marginEndDp = 0,
                                weightVal = 0.85f
                            ) {
                                trashConfirmExpanded = false
                                selectedSftpPaths.clear()
                                rebuildUniversalTermPill(true)
                                activeSftpPopulateRowsCallback?.invoke(null, lastRenderedEntries)
                            }
                        )
                    }
                    2 -> {
                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.TERMINAL,
                                label = "${selectedPackageNames.size}",
                                leftR = pillOuterR,
                                rightR = pillInnerR,
                                fillCol = uiAccentContainer,
                                tintCol = uiOnAccentContainer,
                                weightVal = 1.0f,
                                minW = dp(54),
                                captureLabelRef = true
                            ) {
                                exitToTerminalAnimated()
                            }
                        )
                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.SELECT_ALL,
                                label = tr("Все", "All"),
                                leftR = pillInnerR,
                                rightR = pillInnerR,
                                fillCol = uiSurfaceHigh,
                                tintCol = COLOR_TEXT_BRIGHT,
                                weightVal = 1.15f
                            ) {
                                trashConfirmExpanded = false
                                if (selectedPackageNames.size >= lastUserPackageNames.size && lastUserPackageNames.isNotEmpty()) {
                                    selectedPackageNames.clear()
                                    rebuildUniversalTermPill(true)
                                } else {
                                    selectedPackageNames.addAll(lastUserPackageNames)
                                    rebuildUniversalTermPill(false)
                                }
                                activePackagesPopulateCallback?.invoke()
                            }
                        )
                        val pkgTrashBtn = LinearLayout(this@TermuxActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                            background = makeConnectedSegmentDrawable(
                                COLOR_ROOT_CONTAINER,
                                if (trashConfirmExpanded) pillOuterR else pillInnerR,
                                if (trashConfirmExpanded) pillOuterR else pillInnerR
                            )
                            setPadding(dp(12), 0, dp(12), 0)
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                pillInnerH,
                                if (trashConfirmExpanded) 2.1f else 1.25f
                            ).apply {
                                marginEnd = dp(2)
                            }
                            addView(
                                MaterialIconView(this@TermuxActivity, M3Icon.TRASH, COLOR_ROOT, 17.5f, 2.0f).apply {
                                    layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                                        marginEnd = dp(5)
                                    }
                                }
                            )
                            addView(TextView(this@TermuxActivity).apply {
                                text = if (trashConfirmExpanded) tr("Уверены?", "Sure?") else tr("Удалить", "Remove")
                                setSingleLine(true)
                                setTextColor(COLOR_ROOT)
                                typeface = Typeface.DEFAULT_BOLD
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            })
                            attachSmoothPressAnimation(this, 0.93f)
                            setOnClickListener {
                                if (!trashConfirmExpanded) {
                                    triggerHaptic(1.1f)
                                    trashConfirmExpanded = true
                                    rebuildUniversalTermPill(true)
                                } else {
                                    triggerHaptic(1.35f)
                                    val pkgsToRemove = selectedPackageNames.joinToString(" ")
                                    selectedPackageNames.clear()
                                    trashConfirmExpanded = false
                                    dlg.dismiss()
                                    nativeSendText("apt remove -y $pkgsToRemove\\n", false)
                                }
                            }
                        }
                        contentRow.addView(pkgTrashBtn)

                        contentRow.addView(
                            makeSegmentPill(
                                icon = M3Icon.CLOSE,
                                label = null,
                                leftR = pillInnerR,
                                rightR = pillOuterR,
                                fillCol = uiSurfaceHigh,
                                tintCol = COLOR_TEXT_BRIGHT,
                                marginEndDp = 0,
                                weightVal = 0.85f
                            ) {
                                trashConfirmExpanded = false
                                selectedPackageNames.clear()
                                rebuildUniversalTermPill(true)
                                activePackagesPopulateCallback?.invoke()
                            }
                        )
                    }
                    3 -> {
                        val termDrawerChip = makeSegmentPill(
                            icon = M3Icon.TERMINAL,
                            label = null,
                            leftR = pillOuterR,
                            rightR = pillInnerR,
                            fillCol = uiAccentContainer,
                            tintCol = uiOnAccentContainer,
                            marginEndDp = 3,
                            weightVal = 0f,
                            minW = dp(36)
                        ) {
                            exitToTerminalAnimated()
                        }
                        contentRow.addView(termDrawerChip)

                        val curSrc = currentSftpSourceCode()
                        val srcHotbarOuter = FrameLayout(this@TermuxActivity).apply {
                            clipChildren = true
                            clipToPadding = true
                            layoutParams = LinearLayout.LayoutParams(0, pillInnerH, 1f).apply {
                                marginEnd = dp(3)
                            }
                        }
                        val srcSlidingPill = View(this@TermuxActivity).apply {
                            background = makeLiquidGlassPillDrawable(activePillColor, pillOuterR.roundToInt())
                            layoutParams = FrameLayout.LayoutParams(0, pillInnerH)
                        }
                        srcHotbarOuter.addView(srcSlidingPill)

                        val srcHotbarRow = LinearLayout(this@TermuxActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                pillInnerH
                            )
                        }
                        srcHotbarOuter.addView(srcHotbarRow)

                        val sources = listOf(
                            Triple(0, M3Icon.FOLDER, "Termux"),
                            Triple(1, M3Icon.STORAGE, "SD"),
                            Triple(2, M3Icon.ROOT_HASH, "Root"),
                            Triple(3, M3Icon.SERVER, "SSH")
                        )
                        val srcTabViews = ArrayList<LinearLayout>(4)
                        val srcIcons = ArrayList<MaterialIconView>(4)
                        val srcLabels = ArrayList<TextView>(4)
                        val srcSelProgs = FloatArray(4) { idx -> if (idx == curSrc) 1f else 0f }
                        var srcSwitchAnim: ValueAnimator? = null

                        fun updateSrcHotbarGeometry() {
                            for (idx in 0 until 4) {
                                val sf = safeClamp(srcSelProgs[idx], 0f, 1f)
                                val ic = srcIcons[idx]
                                val icLp = ic.layoutParams as LinearLayout.LayoutParams
                                val icW = (dpF(15f) * sf).roundToInt()
                                val icM = (dpF(4f) * sf).roundToInt()
                                ic.visibility = View.VISIBLE
                                ic.alpha = sf
                                ic.scaleX = 0.65f + 0.35f * sf
                                ic.scaleY = 0.65f + 0.35f * sf
                                if (icLp.width != icW || icLp.marginEnd != icM) {
                                    icLp.width = icW
                                    icLp.marginEnd = icM
                                    ic.layoutParams = icLp
                                }
                                val box = srcTabViews[idx]
                                val lp = box.layoutParams as LinearLayout.LayoutParams
                                val targetW = 0.86f + 0.50f * sf
                                if (abs(lp.weight - targetW) > 0.004f) {
                                    lp.weight = targetW
                                    box.layoutParams = lp
                                }
                                srcLabels[idx].setTextColor(blendColor(COLOR_TEXT_DIM, COLOR_TEXT_BRIGHT, sf))
                            }
                            (srcSlidingPill.tag as? LiquidPillTrackerState)?.let { st ->
                                syncLiquidPillGeometryNow(srcSlidingPill, srcHotbarOuter, st)
                            }
                        }

                        fun selectSourceAnimated(sCode: Int, targetBox: LinearLayout, onSwitched: () -> Unit) {
                            srcSwitchAnim?.cancel()
                            val startProgs = FloatArray(4) { srcSelProgs[it] }
                            srcSwitchAnim = animEngine.drive("srcSwitch", 230L, 0, onStep = { t ->
                                val p = safeClamp(t, 0f, 1f)
                                for (i in 0 until 4) {
                                    val goal = if (i == sCode) 1f else 0f
                                    srcSelProgs[i] = startProgs[i] + (goal - startProgs[i]) * p
                                }
                                updateSrcHotbarGeometry()
                            })
                            animateLiquidSegmentedPill(srcSlidingPill, srcHotbarOuter, targetBox, animate = true)
                            srcHotbarRow.postDelayed({
                                onSwitched()
                            }, 175L)
                        }

                        for ((sCode, sIcon, sLbl) in sources) {
                            val isSel = (curSrc == sCode)
                            val ic = MaterialIconView(this@TermuxActivity, sIcon, uiOnAccentContainer, 15f, 2.0f).apply {
                                layoutParams = LinearLayout.LayoutParams(if (isSel) dp(15) else 0, dp(15)).apply {
                                    marginEnd = if (isSel) dp(4) else 0
                                }
                                alpha = if (isSel) 1f else 0f
                            }
                            val tv = TextView(this@TermuxActivity).apply {
                                text = sLbl
                                setSingleLine(true)
                                setTextColor(if (isSel) COLOR_TEXT_BRIGHT else COLOR_TEXT_DIM)
                                typeface = Typeface.DEFAULT_BOLD
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.8f)
                            }
                            val box = LinearLayout(this@TermuxActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                setPadding(dp(4), 0, dp(4), 0)
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    pillInnerH,
                                    if (isSel) 1.36f else 0.86f
                                )
                                addView(ic)
                                addView(tv)
                                attachSmoothPressAnimation(this, 0.94f)
                                setOnClickListener {
                                    triggerHaptic(0.85f)
                                    when (sCode) {
                                        0 -> {
                                            selectSourceAnimated(0, this) {
                                                activeSftpServer = null
                                                localCurrentDir = appRootDir
                                                prefs.edit().putString(KEY_SFTP_LAST_SAVED_PATH, localCurrentDir.absolutePath).apply()
                                                termDrawerExpanded = false
                                                rebuildUniversalTermPill(true)
                                                activeSftpBreadcrumbsCallback?.invoke(true)
                                                activeSftpRefreshCallback?.invoke(false, null)
                                            }
                                        }
                                        1 -> {
                                            val switchToSd = {
                                                selectSourceAnimated(1, this) {
                                                    activeSftpServer = null
                                                    localCurrentDir = sdDirGlobal
                                                    prefs.edit().putString(KEY_SFTP_LAST_SAVED_PATH, localCurrentDir.absolutePath).apply()
                                                    termDrawerExpanded = false
                                                    rebuildUniversalTermPill(true)
                                                    activeSftpBreadcrumbsCallback?.invoke(true)
                                                    activeSftpRefreshCallback?.invoke(false, null)
                                                }
                                            }
                                            if (!hasAllFilesAccessPermission() && !hasStandardStoragePermission()) {
                                                showStoragePermissionsModal { switchToSd() }
                                            } else {
                                                switchToSd()
                                            }
                                        }
                                        2 -> {
                                            selectSourceAnimated(2, this) {
                                                activeSftpServer = null
                                                localCurrentDir = fsRootDir
                                                prefs.edit().putString(KEY_SFTP_LAST_SAVED_PATH, localCurrentDir.absolutePath).apply()
                                                termDrawerExpanded = false
                                                rebuildUniversalTermPill(true)
                                                activeSftpBreadcrumbsCallback?.invoke(true)
                                                activeSftpRefreshCallback?.invoke(false, null)
                                            }
                                        }
                                        3 -> {
                                            val saved = loadSavedSftpServers()
                                            if (saved.isEmpty()) {
                                                showSftpServerConfigModal(null) { srvNew ->
                                                    selectSourceAnimated(3, this) {
                                                        activeSftpServer = srvNew
                                                        remoteCurrentPath = srvNew.remotePath
                                                        termDrawerExpanded = false
                                                        rebuildUniversalTermPill(true)
                                                        activeSftpBreadcrumbsCallback?.invoke(true)
                                                        activeSftpRefreshCallback?.invoke(false, null)
                                                    }
                                                }
                                            } else if (saved.size == 1 && activeSftpServer == null) {
                                                val onlySrv = saved[0]
                                                selectSourceAnimated(3, this) {
                                                    activeSftpServer = onlySrv
                                                    remoteCurrentPath = onlySrv.remotePath
                                                    termDrawerExpanded = false
                                                    rebuildUniversalTermPill(true)
                                                    activeSftpBreadcrumbsCallback?.invoke(true)
                                                    activeSftpRefreshCallback?.invoke(false, null)
                                                }
                                            } else {
                                                showSftpServersListModal { chosenSrv ->
                                                    selectSourceAnimated(3, this) {
                                                        activeSftpServer = chosenSrv
                                                        remoteCurrentPath = chosenSrv.remotePath
                                                        termDrawerExpanded = false
                                                        rebuildUniversalTermPill(true)
                                                        activeSftpBreadcrumbsCallback?.invoke(true)
                                                        activeSftpRefreshCallback?.invoke(false, null)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            srcTabViews.add(box)
                            srcIcons.add(ic)
                            srcLabels.add(tv)
                            srcHotbarRow.addView(box)
                        }
                        srcHotbarRow.post {
                            val curTargetBox = srcTabViews.getOrNull(curSrc)
                            if (curTargetBox != null) {
                                animateLiquidSegmentedPill(srcSlidingPill, srcHotbarOuter, curTargetBox, animate = false)
                            }
                        }
                        contentRow.addView(srcHotbarOuter)

                        val closeChip = makeSegmentPill(
                            icon = M3Icon.CLOSE,
                            label = null,
                            leftR = pillInnerR,
                            rightR = pillOuterR,
                            fillCol = uiSurfaceHigh,
                            tintCol = COLOR_TEXT_BRIGHT,
                            marginEndDp = 0,
                            weightVal = 0f,
                            minW = dp(34)
                        ) {
                            termDrawerExpanded = false
                            rebuildUniversalTermPill(true)
                        }
                        contentRow.addView(closeChip)
                    }
                    else -> {
                        // Mode 0: Default Compact State
                        val showSourceAccessory = (activeTab == 1)
                        val wantAccessory = hasPasteClipboard || showSourceAccessory
                        val mainLeftR = pillOuterR
                        val mainRightR = if (wantAccessory) pillInnerR else pillOuterR
                        val termBackChipBg = makeConnectedSegmentDrawable(uiAccentContainer, mainLeftR, mainRightR)

                        val termBackIcon = MaterialIconView(this@TermuxActivity, M3Icon.TERMINAL, uiOnAccentContainer, 18f, 2.0f).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(19), dp(19)).apply {
                                marginEnd = dp(5)
                            }
                        }
                        val termBackLabel = TextView(this@TermuxActivity).apply {
                            text = tr("Терминал", "Terminal")
                            setSingleLine(true)
                            setTextColor(uiOnAccentContainer)
                            typeface = Typeface.DEFAULT_BOLD
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                        }
                        var swipeStartX = 0f
                        var swipeTriggered = false
                        val termBackChip = LinearLayout(this@TermuxActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                            background = termBackChipBg
                            setPadding(dp(12), 0, dp(13), 0)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                pillInnerH
                            ).apply {
                                if (wantAccessory) marginEnd = dp(2)
                            }
                            addView(termBackIcon)
                            addView(termBackLabel)
                            attachSmoothPressAnimation(this, 0.94f)
                            if (activeTab == 1) {
                                setOnTouchListener { _, ev ->
                                    when (ev.actionMasked) {
                                        MotionEvent.ACTION_DOWN -> {
                                            swipeStartX = ev.rawX
                                            swipeTriggered = false
                                            false
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            if (!swipeTriggered && (ev.rawX - swipeStartX) > dpF(26f)) {
                                                swipeTriggered = true
                                                triggerHaptic(1.0f)
                                                termDrawerExpanded = true
                                                rebuildUniversalTermPill(true)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        else -> swipeTriggered
                                    }
                                }
                                setOnLongClickListener {
                                    triggerHaptic(1.15f)
                                    termDrawerExpanded = true
                                    rebuildUniversalTermPill(true)
                                    true
                                }
                            }
                            setOnClickListener {
                                if (!swipeTriggered) {
                                    exitToTerminalAnimated()
                                }
                            }
                        }
                        contentRow.addView(termBackChip)

                        if (wantAccessory) {
                            val accessoryPill = createAccessoryPillView()
                            contentRow.addView(accessoryPill)
                        }
                    }
                }
                return contentRow
            }

            val prevMode = currentTermPillMode
            currentTermPillMode = targetMode
            lastTermPillTrashConfirm = trashConfirmExpanded
            lastTermPillHasClip = hasPasteClipboard
            lastTermPillSrvNull = srvNull
            lastTermPillSrcCode = srcCodeNow
            termPillCountTv = null

            termExpandAnim?.cancel()
            termAccessoryAnim?.cancel()

            val startTermW = termOuterFrame.width

            if (animateMorph && prevMode > 0 && targetMode == 0 && termOuterFrame.childCount > 0 && startTermW > dp(140)) {
                // Smooth collapse / fold into compact capsule
                val oldRow = termOuterFrame.getChildAt(termOuterFrame.childCount - 1)
                val newRow = buildContentRow(0)
                newRow.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(pillInnerH, View.MeasureSpec.EXACTLY)
                )
                val exactCompactW = newRow.measuredWidth + termOuterFrame.paddingLeft + termOuterFrame.paddingRight
                newRow.alpha = 0f
                newRow.scaleX = 0.94f
                newRow.scaleY = 0.94f
                termOuterFrame.addView(newRow)

                hubTabsOuter.visibility = View.VISIBLE
                hubTabsOuter.alpha = 0f
                hubTabsOuter.scaleX = 0.94f
                hubTabsOuter.scaleY = 0.94f

                termExpandAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 260L
                    interpolator = PathInterpolator(0.12f, 1.0f, 0.24f, 1.0f)
                    addUpdateListener { va ->
                        val p = safeClamp(va.animatedValue as Float, 0f, 1f)
                        val curW = (startTermW + (exactCompactW - startTermW) * p).roundToInt()
                        val lp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                        lp.width = curW
                        lp.weight = 0f
                        lp.marginEnd = (dpF(8f) * p).roundToInt()
                        termOuterFrame.layoutParams = lp

                        oldRow.alpha = (1f - p * 1.6f).coerceIn(0f, 1f)
                        oldRow.translationX = -dpF(14f) * p

                        val pNew = safeClamp((p - 0.10f) / 0.90f, 0f, 1f)
                        newRow.alpha = pNew
                        newRow.scaleX = 0.94f + 0.06f * pNew
                        newRow.scaleY = 0.94f + 0.06f * pNew

                        val pTab = safeClamp((p - 0.08f) / 0.92f, 0f, 1f)
                        hubTabsOuter.alpha = pTab
                        hubTabsOuter.scaleX = 0.94f + 0.06f * pTab
                        hubTabsOuter.scaleY = 0.94f + 0.06f * pTab
                        (hubSlidingPill.tag as? LiquidPillTrackerState)?.let { st ->
                            syncLiquidPillGeometryNow(hubSlidingPill, hubTabsOuter, st)
                        }
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            while (termOuterFrame.childCount > 1) {
                                termOuterFrame.removeViewAt(0)
                            }
                            newRow.alpha = 1f
                            newRow.scaleX = 1f
                            newRow.scaleY = 1f
                            newRow.translationX = 0f
                            val finalLp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                            finalLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                            finalLp.weight = 0f
                            finalLp.marginEnd = dp(8)
                            termOuterFrame.layoutParams = finalLp
                            hubTabsOuter.alpha = 1f
                            hubTabsOuter.scaleX = 1f
                            hubTabsOuter.scaleY = 1f
                            (hubSlidingPill.tag as? LiquidPillTrackerState)?.let { st ->
                                syncLiquidPillGeometryNow(hubSlidingPill, hubTabsOuter, st)
                            }
                        }
                    })
                    start()
                }
            } else if (animateMorph && prevMode == 0 && targetMode > 0 && termOuterFrame.childCount > 0) {
                // Smooth unfold / expand into full drawer / action bar
                val oldRow = termOuterFrame.getChildAt(termOuterFrame.childCount - 1)
                val totalNavW = (topNavRow.width - topNavRow.paddingLeft - topNavRow.paddingRight)
                    .takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - dp(28))
                val newRow = buildContentRow(targetMode)
                newRow.alpha = 0f
                newRow.translationX = dpF(12f)
                termOuterFrame.addView(newRow)

                termExpandAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 270L
                    interpolator = PathInterpolator(0.12f, 1.0f, 0.24f, 1.0f)
                    addUpdateListener { va ->
                        val p = safeClamp(va.animatedValue as Float, 0f, 1f)
                        val curW = (startTermW + (totalNavW - startTermW) * p).roundToInt()
                        val lp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                        lp.width = curW
                        lp.weight = 0f
                        lp.marginEnd = (dpF(8f) * (1f - p)).roundToInt()
                        termOuterFrame.layoutParams = lp

                        oldRow.alpha = (1f - p * 1.5f).coerceIn(0f, 1f)

                        val pNew = safeClamp((p - 0.12f) / 0.88f, 0f, 1f)
                        newRow.alpha = pNew
                        newRow.translationX = dpF(12f) * (1f - pNew)

                        hubTabsOuter.alpha = (1f - p * 1.45f).coerceIn(0f, 1f)
                        hubTabsOuter.scaleX = 1f - 0.06f * p
                        hubTabsOuter.scaleY = 1f - 0.06f * p
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            while (termOuterFrame.childCount > 1) {
                                termOuterFrame.removeViewAt(0)
                            }
                            newRow.alpha = 1f
                            newRow.translationX = 0f
                            val finalLp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                            finalLp.width = 0
                            finalLp.weight = 1f
                            finalLp.marginEnd = 0
                            termOuterFrame.layoutParams = finalLp
                            hubTabsOuter.visibility = View.GONE
                            hubTabsOuter.alpha = 1f
                            hubTabsOuter.scaleX = 1f
                            hubTabsOuter.scaleY = 1f
                        }
                    })
                    start()
                }
            } else if (animateMorph && prevMode == 0 && targetMode == 0 && termOuterFrame.childCount > 0) {
                // Smooth cross-fade between compact modes (e.g. tab switch where accessory changes)
                val oldRow = termOuterFrame.getChildAt(termOuterFrame.childCount - 1)
                val newRow = buildContentRow(0)
                newRow.alpha = 0f
                termOuterFrame.addView(newRow)

                termExpandAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 180L
                    addUpdateListener { va ->
                        val p = safeClamp(va.animatedValue as Float, 0f, 1f)
                        oldRow.alpha = 1f - p
                        newRow.alpha = p
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            while (termOuterFrame.childCount > 1) {
                                termOuterFrame.removeViewAt(0)
                            }
                            newRow.alpha = 1f
                            val finalLp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                            finalLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                            finalLp.weight = 0f
                            finalLp.marginEnd = dp(8)
                            termOuterFrame.layoutParams = finalLp
                        }
                    })
                    start()
                }
            } else {
                // Instant update / initial setup
                termOuterFrame.removeAllViews()
                val newRow = buildContentRow(targetMode)
                termOuterFrame.addView(newRow)
                val lp = termOuterFrame.layoutParams as LinearLayout.LayoutParams
                if (targetMode != 0) {
                    lp.width = 0
                    lp.weight = 1f
                    lp.marginEnd = 0
                    hubTabsOuter.visibility = View.GONE
                } else {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    lp.weight = 0f
                    lp.marginEnd = dp(8)
                    hubTabsOuter.visibility = View.VISIBLE
                    hubTabsOuter.alpha = 1f
                    hubTabsOuter.scaleX = 1f
                    hubTabsOuter.scaleY = 1f
                    hubTabsRow.post {
                        val activeView = hubTabContainers.getOrNull(activeTab)
                        if (activeView != null) {
                            (hubSlidingPill.tag as? LiquidPillTrackerState)?.let { st ->
                                syncLiquidPillGeometryNow(hubSlidingPill, hubTabsOuter, st)
                            }
                        }
                    }
                }
                termOuterFrame.layoutParams = lp
            }
        }
'''

    new_content = content[:start_idx] + replacement_code + "\n" + content[end_idx:]
    with open(target_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print("Successfully patched TermuxActivity.kt with clean FrameLayout crossfade architecture!")

if __name__ == '__main__':
    main()
