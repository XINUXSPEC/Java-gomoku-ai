package AIArena;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ArenaDataPacker {

    /**
     * 将比赛数据打包，生成带有标准数学收敛曲线的 HTML 网页
     * @param labelA 选手A的完整描述（包含层数/参数）
     * @param labelB 选手B的完整描述（包含层数/参数）
     */
    public static void generateReport(String labelA, String labelB, List<Double> winRateHistoryA, List<Double> winRateHistoryB) {
        StringBuilder xData = new StringBuilder();
        StringBuilder yDataA = new StringBuilder();
        StringBuilder yDataB = new StringBuilder();

        for (int i = 0; i < winRateHistoryA.size(); i++) {
            xData.append("'第 ").append(i + 1).append(" 局'");
            yDataA.append(String.format("%.2f", winRateHistoryA.get(i) * 100));
            yDataB.append(String.format("%.2f", winRateHistoryB.get(i) * 100));
            if (i < winRateHistoryA.size() - 1) {
                xData.append(", ");
                yDataA.append(", ");
                yDataB.append(", ");
            }
        }

        String htmlTemplate = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>AI Arena 数学战力收敛曲线</title>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js\"></script>\n" +
                "    <style>body { font-family: sans-serif; background: #f4f6f9; padding: 20px; } .card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 1000px; margin: 0 auto; }</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <h2>🏆 AI 擂台数学胜率收敛走势图</h2>\n" +
                "        <p>当前对战配置：<b>" + labelA + "</b>  VS  <b>" + labelB + "</b></p>\n" +
                "        <p>此曲线展示了随着对局数（样本量）的增加，双方<b>累计实时胜率</b>的数学收敛过程。</p>\n" +
                "        <div id=\"main\" style=\"width: 100%;height:500px;\"></div>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        var chartDom = document.getElementById('main');\n" +
                "        var myChart = echarts.init(chartDom);\n" +
                "        var option = {\n" +
                "            title: { text: '实时累计胜率收敛曲线 (单位: %)' },\n" +
                "            tooltip: { trigger: 'axis' },\n" +
                "            legend: { data: ['" + labelA + "', '" + labelB + "'] },\n" +
                "            grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },\n" +
                "            xAxis: { type: 'category', boundaryGap: false, data: [" + xData + "] },\n" +
                "            yAxis: { type: 'value', min: 0, max: 100 },\n" +
                "            series: [\n" +
                "                { name: '" + labelA + "', type: 'line', smooth: true, data: [" + yDataA + "], lineStyle: { width: 3 } },\n" +
                "                { name: '" + labelB + "', type: 'line', smooth: true, data: [" + yDataB + "], lineStyle: { width: 3 } }\n" +
                "            ]\n" +
                "        };\n" +
                "        myChart.setOption(option);\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        try (FileWriter writer = new FileWriter("arena_report.html")) {
            writer.write(htmlTemplate);
            System.out.println("\n📈 [数学曲线已绘制] 战力分析数据已成功导出为数学曲线网页！");
            System.out.println("👉 请直接双击项目根目录下的 [ arena_report.html ] 查看带调参参数的超清走势图。");
        } catch (IOException e) {
            System.err.println("无法写入曲线报表文件。");
            e.printStackTrace();
        }
    }
}