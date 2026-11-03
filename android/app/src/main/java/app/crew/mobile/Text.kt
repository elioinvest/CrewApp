package app.crew.mobile

import java.util.Locale

// The app's words in English and Chinese, chosen by the phone's language. Text is written in
// English in the code and looked up here, as the web version does it.
object Text {
    val zh: Boolean get() = Locale.getDefault().language == "zh"

    private val ZH = mapOf(
        "Use Crew from your phone" to "在手机上使用 Crew",
        "In Crew on your computer, open Settings → Phone access, turn it on, then scan its QR code." to "在电脑上的 Crew 中打开 设置 → 手机访问，开启后扫描其中的二维码。",
        "Scan QR code" to "扫描二维码", "Connect" to "连接", "Address, like 192.168.1.20:7788" to "地址，例如 192.168.1.20:7788",
        "Pairing code" to "配对码", "Computers" to "电脑", "Forget" to "忘记",
        "Connecting to %s…" to "正在连接 %s…",
        "Couldn't reach %s. Is Crew running with phone access on, and is this phone on the same Wi-Fi (or Tailscale)?" to "无法连接 %s。请确认 Crew 正在运行并已开启手机访问，且手机与电脑在同一 Wi-Fi（或 Tailscale）下。",
        "Enter the address and the pairing code shown in Crew." to "请输入 Crew 中显示的地址和配对码。",
        "That isn't Crew's pairing code." to "这不是 Crew 的配对码。",
        "What are we doing today?" to "今天做点什么？", "New chat" to "新会话", "Agent runs" to "智能体运行",
        "Projects" to "项目", "Hosts" to "主机", "Agents" to "智能体", "Search projects and sessions" to "搜索项目和会话",
        "No sessions yet" to "还没有会话", "No matches" to "没有匹配项", "No projects yet" to "还没有项目", "Show %d more" to "再显示 %d 个",
        "Add hosts in Crew on your computer." to "在电脑上的 Crew 中添加主机。",
        "Add agents in Crew on your computer, then @-mention them in a chat." to "在电脑上的 Crew 中添加智能体，然后在会话中 @ 它们。",
        "reads" to "只读", "edits" to "可编辑", "full access" to "完全访问",
        "Describe the task to start a new chat" to "描述要做的事来开始新会话", "Message, or / for commands" to "发消息，/ 调用指令",
        "Choose a project" to "选择项目", "Agent" to "智能体", "Commands" to "指令", "Model" to "模型", "Effort" to "推理强度", "Mode" to "模式",
        "Loading…" to "加载中…", "Try again when the agent is idle." to "请在智能体空闲时再试。", "Switching…" to "正在切换…",
        "Needs your OK" to "需要确认", "Deny" to "拒绝", "Allow 15 min" to "允许 15 分钟", "Allow" to "允许",
        "An agent wants to %s %s" to "智能体想要%s %s", "run on" to "运行命令，主机", "upload a file to" to "上传文件到",
        "download a file from" to "从其下载文件", "type into your terminal on" to "在你的终端中输入，主机",
        "%s is waiting for you on the computer" to "%s 正在电脑上等你处理",
        "Running" to "运行中", "Done" to "已完成", "Failed" to "失败", "Stop" to "停止", "No agent runs yet." to "还没有智能体运行。",
        "%d runs · %d running" to "%d 次运行 · %d 个进行中",
        "Pair another computer" to "配对另一台电脑", "Disconnect" to "断开连接", "Connected" to "已连接", "Not connected" to "未连接",
        "Starting a %s session in Crew…" to "正在 Crew 中启动 %s 会话…", "Opening it in Crew…" to "正在 Crew 中打开…",
        "Finished" to "已完成", "Finished: %s" to "已完成：%s", "Sending…" to "正在发送…", "Added. Say what to do with it, then send." to "已添加。说明要怎么处理，然后发送。",
        "Files up to 20 MB can be sent." to "只能发送 20 MB 以内的文件。", "The computer didn't answer." to "电脑没有响应。",
        "Working" to "工作中", "Ran %d steps" to "执行了 %d 个操作", "Ran a step" to "执行了 1 个操作",
        "The agent is asking" to "智能体在询问", "Keys" to "按键", "Image" to "图片", "Images: %d" to "图片：%d",
        "This chat isn't open in Crew. Sending a message opens it there." to "这个会话没有在 Crew 中打开。发送消息时会在 Crew 中打开它。",
        "Start a chat, or pick one from the list." to "开始新会话，或从列表中选择一个。",
        "Switch model" to "切换模型", "Summarize to free context" to "压缩上下文", "Start over" to "清空重来", "Context used" to "上下文用量",
        "Plan usage" to "套餐用量", "Resume a session" to "恢复会话", "All commands" to "全部指令", "Model and reasoning" to "模型与推理强度",
        "Approval mode" to "审批模式", "New conversation" to "新对话", "Changes so far" to "目前的改动", "Review changes" to "审查改动", "Session status" to "会话状态",
        "Untitled" to "未命名", "now" to "刚刚", "%dm" to "%d分钟", "%dh" to "%d小时", "%dd" to "%d天",
        "Manual" to "手动", "Accept edits" to "自动接受编辑", "Plan" to "计划", "Auto" to "自动", "Bypass" to "跳过权限",
        "Low" to "低", "Medium" to "中", "High" to "高", "Extra high" to "超高", "Max" to "最高", "Permissions" to "权限",
        "Ask for approval" to "请求批准", "Approve for me" to "替我批准", "Full Access" to "完全访问", "Read Only" to "只读",
        "Refresh model list" to "刷新模型列表", "Older models" to "较早的模型", "On request" to "按需", "Untrusted" to "不受信任", "On failure" to "失败时", "Never ask" to "从不询问", "Don't ask" to "不询问", "Default" to "默认", "Default (the model's)" to "默认（模型自身）",
        "Connected to %s" to "已连接到 %s", "Crew connection" to "Crew 连接", "Approvals and finished tasks" to "审批与完成的任务",
        "This Crew is too old to show chats. Update Crew on your computer to 2.5.58 or later." to "电脑上的 Crew 版本太旧，无法显示会话。请把 Crew 更新到 2.5.58 或更高版本。",
        "No messages in this chat yet." to "这个会话还没有消息。",
        "Copy" to "复制", "Copied" to "已复制", "Send" to "发送", "Attach" to "附件", "Back" to "返回",
    )

    fun t(s: String, vararg args: Any): String {
        val f = if (zh) ZH[s] ?: s else s
        return if (args.isEmpty()) f else String.format(f, *args)
    }
}

fun t(s: String, vararg args: Any) = Text.t(s, *args)
