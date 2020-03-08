# EHub---TempData
Following the prompt at https://gist.github.com/jdangerx/883b9057c5af65e6965acc646f445c3e

<h3> How to run: </h3> 

<h3> From terminal/command line: </h3> 
Clone or download this project and navigate to out/production/ehub_tempdata.



<h3> From an IDE Console: </h3> 

Pass requested arguments to the execute method in main and run
Enter into terminal any field(s) and parameters to search:
<pre><code>java com.company.ehub_tempdata.TempUpdateApp

tempupdate --field ambientTemp /tmp/ehub_data 2016-01-01T03:00
</code></pre>

<br></br>

<pre><code> 
String[] tester = {"--field", "ambientTemp", "--field", "schedule", "/tmp/ehub_data", "2016-01-01T09:34"};

public static void main(String[] args) {</br>
   new CommandLine(new TempUpdate()).execute(tester);</br>
} 
</code></pre>
